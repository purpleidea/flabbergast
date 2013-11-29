#!/bin/sh

test -d m4 || mkdir m4
./build-aux/gitlog-to-changelog

autoreconf -i
