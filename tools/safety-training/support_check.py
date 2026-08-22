import sys

from sparrow_safety_training.cli import main


if __name__ == "__main__":
    main(["support-check", *sys.argv[1:]])
