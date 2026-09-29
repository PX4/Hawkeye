#!/usr/bin/env python3
"""Renders the Homebrew formula for Hawkeye to stdout.

Used twice in a desktop release: once without a bottle block, pointing at a local copy of
the source tarball, to build the bottle (homebrew-release.yml); and once with the bottle
block, pointing at the published release, for the tap (update-tap.sh). Keeping
one template means the formula that builds the bottle and the one users install from
cannot drift apart.
"""

import argparse

FORMULA = """\
class Hawkeye < Formula
  desc "Real-time 3D flight visualizer for PX4 with ULog replay and multi-drone analysis"
  homepage "https://github.com/PX4/Hawkeye"
  url "{url}"
  sha256 "{sha256}"
  license "BSD-3-Clause"
{bottle}
  depends_on "cmake" => :build

  def install
    system "cmake", "-S", ".", "-B", "build",
           "-DCMAKE_BUILD_TYPE=Release",
           "-DHOMEBREW_ALLOW_FETCHCONTENT=ON",
           *std_cmake_args
    system "cmake", "--build", "build"
    system "cmake", "--install", "build", "--prefix", prefix
  end

  test do
    assert_predicate bin/"hawkeye", :executable?
  end
end
"""

BOTTLE = """
  bottle do
    root_url "{root_url}"
    sha256 cellar: :any_skip_relocation, {tag}: "{sha256}"
  end
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--url", required=True, help="source tarball URL")
    parser.add_argument("--sha256", required=True, help="source tarball SHA-256")
    parser.add_argument("--bottle-root-url", help="URL the bottle is downloaded from")
    parser.add_argument("--bottle-tag", help="bottle tag, such as arm64_sonoma")
    parser.add_argument("--bottle-sha256", help="bottle SHA-256")
    args = parser.parse_args()

    bottle_args = (args.bottle_root_url, args.bottle_tag, args.bottle_sha256)
    if any(bottle_args) and not all(bottle_args):
        parser.error("--bottle-root-url, --bottle-tag, and --bottle-sha256 go together")

    bottle = ""
    if all(bottle_args):
        bottle = BOTTLE.format(
            root_url=args.bottle_root_url, tag=args.bottle_tag, sha256=args.bottle_sha256
        )

    print(FORMULA.format(url=args.url, sha256=args.sha256, bottle=bottle), end="")


if __name__ == "__main__":
    main()
