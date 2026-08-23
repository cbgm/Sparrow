#!/usr/bin/env python3
import sys

from sparrow_safety_training.cli import main

if __name__ == "__main__":
    main(["quality-gate", *sys.argv[1:]])
