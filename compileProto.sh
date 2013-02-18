#!/bin/sh
#Compile protobuf message to Java and put the files in the sources folder
cd src/main/protobuf
DIRS="entropy/configuration/parser/"
for d in $DIRS; do
    echo $d
    protoc --java_out=../java/ -I. $d/*.proto
done
cd -