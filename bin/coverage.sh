#!/bin/bash

while getopts "p:r:m:" flag
do
  case $flag in
    m) maxpoints=$OPTARG;;
    r) region=$OPTARG;;
    p) positions=$OPTARG;;
  esac
done

SAMTOOLS=$HOME/work/iobio/bin/samtools

JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0
CLASSPATH=/Users/tonyd/work/iobio/bin

$SAMTOOLS mpileup - | $JAVA_HOME/Home/bin/java -cp $CLASSPATH GetCoverage $maxpoints $region $positions
