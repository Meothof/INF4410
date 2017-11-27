#!/bin/bash

set -m # Enable Job Control
START=$(date +%s.%N)
for i in `seq 40`; do # start 40 jobs in parallel
  curl 172.15.108.209:8000 &
echo "Start $i"
done

# Wait for all parallel jobs to finish
while [ 1 ];
do
  fg 2> /dev/null;
  [ $? == 1 ] && break;
done

END=$(date +%s.%N)
DIFF=$(echo " $END - $START" | bc)
echo "time : "
echo $DIFF
