#include <jni.h>
#include <algorithm>
#include <chrono>
#include <string>
#include <thread>
#include <vector>

#include "whisper.h"

namespace {

whisper_context *asContext(jlong handle) {
    return reinterpret_cast<whisper_context *>(handle);
}

void throwIllegalState(JNIEnv *env, const char *message) {
    jclass exceptionClass = env->FindClass("java/lang/IllegalStateException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
    }
}

constexpr int WHISPER_SAMPLE_RATE_HZ = 16000;
constexpr int DEFAULT_THREAD_COUNT = 4;
constexpr int MAX_THREAD_COUNT = 8;
constexpr int MIN_TRANSCRIPTION_TIMEOUT_SECONDS = 60;
constexpr int MAX_TRANSCRIPTION_TIMEOUT_SECONDS = 180;
constexpr int TIMEOUT_MULTIPLIER = 10;

struct TranscriptionDeadline {
    std::chrono::steady_clock::time_point deadline;
    bool timedOut = false;
};

struct ProgressCallbackContext {
    JNIEnv *env;
    jobject callback;
    jmethodID onProgressMethod;
    int lastProgress;

    ProgressCallbackContext(
        JNIEnv *envValue,
        jobject callbackValue,
        jmethodID onProgressMethodValue
    )
        : env(envValue),
          callback(callbackValue),
          onProgressMethod(onProgressMethodValue),
          lastProgress(-1) {}
};

void reportProgressValue(ProgressCallbackContext *callbackData, int progress) {
    if (callbackData == nullptr || callbackData->callback == nullptr || callbackData->onProgressMethod == nullptr) {
        return;
    }

    // Reserve 100 for Kotlin after whisper_full() actually returns successfully.
    const int normalizedProgress = std::clamp(progress, 0, 99);
    if (normalizedProgress <= callbackData->lastProgress) return;

    callbackData->lastProgress = normalizedProgress;
    callbackData->env->CallVoidMethod(
        callbackData->callback,
        callbackData->onProgressMethod,
        static_cast<jint>(normalizedProgress)
    );
}

void reportChunkProgress(
    struct whisper_context *,
    struct whisper_state *,
    int progress,
    void *userData
) {
    // whisper.cpp reports progress from its internal audio seek position.
    // These callbacks are intentionally sparse for short/medium recordings,
    // because one inference window can cover a large part of the clip.
    // Kotlin interpolates between these real checkpoints for smooth UI progress.
    reportProgressValue(
        static_cast<ProgressCallbackContext *>(userData),
        progress
    );
}

bool abortOnDeadline(void *userData) {
    auto *deadline = static_cast<TranscriptionDeadline *>(userData);
    if (std::chrono::steady_clock::now() < deadline->deadline) return false;

    deadline->timedOut = true;
    return true;
}

int transcriptionTimeoutSeconds(jsize sampleCount) {
    const int audioSeconds =
        std::max(1, static_cast<int>(sampleCount / WHISPER_SAMPLE_RATE_HZ));
    return std::clamp(
        MIN_TRANSCRIPTION_TIMEOUT_SECONDS + audioSeconds * TIMEOUT_MULTIPLIER,
        MIN_TRANSCRIPTION_TIMEOUT_SECONDS,
        MAX_TRANSCRIPTION_TIMEOUT_SECONDS
    );
}

int transcriptionThreadCount() {
    const unsigned int hardwareThreads = std::thread::hardware_concurrency();
    if (hardwareThreads == 0) return DEFAULT_THREAD_COUNT;
    return std::clamp(static_cast<int>(hardwareThreads), 1, MAX_THREAD_COUNT);
}

} // namespace

extern "C"
JNIEXPORT jlong JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_loadModel(
    JNIEnv *env,
    jobject,
    jstring modelPath
) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) return 0;

    whisper_context_params contextParams = whisper_context_default_params();
    whisper_context *context = whisper_init_from_file_with_params(path, contextParams);

    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_transcribe(
    JNIEnv *env,
    jobject,
    jlong modelHandle,
    jfloatArray samplesArray,
    jobject progressCallback
) {
    whisper_context *context = asContext(modelHandle);
    if (context == nullptr || samplesArray == nullptr) {
        throwIllegalState(env, "Voice transcription is not initialized");
        return nullptr;
    }

    const jsize sampleCount = env->GetArrayLength(samplesArray);
    if (sampleCount <= 0) {
        throwIllegalState(env, "Voice message contains no audio samples");
        return nullptr;
    }

    std::vector<float> samples(static_cast<size_t>(sampleCount));
    env->GetFloatArrayRegion(samplesArray, 0, sampleCount, samples.data());
    if (env->ExceptionCheck()) return nullptr;

    TranscriptionDeadline deadline {
        std::chrono::steady_clock::now() +
            std::chrono::seconds(transcriptionTimeoutSeconds(sampleCount))
    };

    jclass progressCallbackClass =
        progressCallback == nullptr ? nullptr : env->GetObjectClass(progressCallback);
    jmethodID onProgressMethod =
        progressCallbackClass == nullptr
            ? nullptr
            : env->GetMethodID(progressCallbackClass, "onProgress", "(I)V");
    if (progressCallbackClass != nullptr) {
        env->DeleteLocalRef(progressCallbackClass);
    }

    ProgressCallbackContext progressCallbackContext(
        env,
        progressCallback,
        onProgressMethod
    );

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;
    params.no_context = true;
    params.single_segment = false;
    params.token_timestamps = true;
    params.max_len = 24;
    params.split_on_word = true;
    params.language = "auto";
    params.n_threads = transcriptionThreadCount();
    params.abort_callback = abortOnDeadline;
    params.abort_callback_user_data = &deadline;
    params.progress_callback = reportChunkProgress;
    params.progress_callback_user_data = &progressCallbackContext;

    const int result = whisper_full(context, params, samples.data(), sampleCount);
    if (result != 0) {
        if (deadline.timedOut) {
            throwIllegalState(env, "Voice transcription timed out");
        } else {
            throwIllegalState(env, "Voice transcription failed");
        }
        return nullptr;
    }

    std::string transcript;
    const int segmentCount = whisper_full_n_segments(context);
    for (int segmentIndex = 0; segmentIndex < segmentCount; ++segmentIndex) {
        const char *segment = whisper_full_get_segment_text(context, segmentIndex);
        if (segment != nullptr) transcript += segment;
    }

    return env->NewStringUTF(transcript.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_segmentCount(
    JNIEnv *,
    jobject,
    jlong modelHandle
) {
    whisper_context *context = asContext(modelHandle);
    return context == nullptr ? 0 : whisper_full_n_segments(context);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_segmentText(
    JNIEnv *env,
    jobject,
    jlong modelHandle,
    jint segmentIndex
) {
    whisper_context *context = asContext(modelHandle);
    if (context == nullptr) return env->NewStringUTF("");
    const char *text = whisper_full_get_segment_text(context, segmentIndex);
    return env->NewStringUTF(text == nullptr ? "" : text);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_segmentStartMilliseconds(
    JNIEnv *,
    jobject,
    jlong modelHandle,
    jint segmentIndex
) {
    whisper_context *context = asContext(modelHandle);
    if (context == nullptr) return 0;
    return static_cast<jlong>(whisper_full_get_segment_t0(context, segmentIndex) * 10);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_segmentEndMilliseconds(
    JNIEnv *,
    jobject,
    jlong modelHandle,
    jint segmentIndex
) {
    whisper_context *context = asContext(modelHandle);
    if (context == nullptr) return 0;
    return static_cast<jlong>(whisper_full_get_segment_t1(context, segmentIndex) * 10);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_cbgm_sparrow_feature_voice_device_WhisperNative_freeModel(
    JNIEnv *,
    jobject,
    jlong modelHandle
) {
    whisper_context *context = asContext(modelHandle);
    if (context != nullptr) {
        whisper_free(context);
    }
}
