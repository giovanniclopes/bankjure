#!/usr/bin/env bash

set -euo pipefail

install_revision="2026.05.05-84a231c"
clojure_tools_version="1.12.4.1629"
clojure_tools_sha256="b30045d2587ebc0bcf1ca2a85c75d71dc56d7c74de7eee6dc7fdefc25f7728ce"

do_usage() {
  echo "Installs the Clojure command line tools."
  echo -e
  echo "Usage:"
  echo "linux-install.sh [-p|--prefix <dir>]"
  exit 1
}

default_prefix_dir="/usr/local"

prefix_dir=$default_prefix_dir
prefix_param=${1:-}
prefix_value=${2:-}
if [[ "$prefix_param" = "-p" || "$prefix_param" = "--prefix" ]]; then
  if [[ -z "$prefix_value" ]]; then
    do_usage
  else
    prefix_dir="$prefix_value"
  fi
fi

tarball="clojure-tools-${clojure_tools_version}.tar.gz"
download_url="https://github.com/clojure/brew-install/releases/download/${clojure_tools_version}/${tarball}"

echo "install-revision ${install_revision}"
echo "Downloading and expanding tar"
curl -L -O -f -m 120 --connect-timeout 5 --retry 5 --retry-connrefused --retry-max-time 60 --no-progress-meter "${download_url}"
echo "${clojure_tools_sha256}  ${tarball}" | sha256sum -c | grep "^${tarball}: OK\$"
tar xzf "${tarball}"

lib_dir="$prefix_dir/lib"
bin_dir="$prefix_dir/bin"
man_dir="$prefix_dir/share/man/man1"
clojure_lib_dir="$lib_dir/clojure"
jar_name="clojure-tools-${clojure_tools_version}.jar"

echo "Installing libs into $clojure_lib_dir"
mkdir -p $bin_dir $man_dir $clojure_lib_dir/libexec
install -m644 clojure-tools/deps.edn "$clojure_lib_dir/deps.edn"
install -m644 clojure-tools/example-deps.edn "$clojure_lib_dir/example-deps.edn"
install -m644 clojure-tools/tools.edn "$clojure_lib_dir/tools.edn"
install -m644 clojure-tools/exec.jar "$clojure_lib_dir/libexec/exec.jar"
install -m644 "clojure-tools/${jar_name}" "$clojure_lib_dir/libexec/${jar_name}"

echo "Installing clojure and clj into $bin_dir"
sed -i -e 's@PREFIX@'"$clojure_lib_dir"'@g' clojure-tools/clojure
sed -i -e 's@BINDIR@'"$bin_dir"'@g' clojure-tools/clj
install -m755 clojure-tools/clojure "$bin_dir/clojure"
install -m755 clojure-tools/clj "$bin_dir/clj"

echo "Installing man pages into $man_dir"
install -m644 clojure-tools/clojure.1 "$man_dir/clojure.1"
install -m644 clojure-tools/clj.1 "$man_dir/clj.1"

echo "Removing download"
rm -rf clojure-tools
rm -rf "${tarball}"

echo "Use clj -h for help."
