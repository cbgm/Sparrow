from __future__ import annotations

import sys

from sparrow_safety_training.cli import main


if __name__ == "__main__":
    main(["evaluate", *sys.argv[1:]])
