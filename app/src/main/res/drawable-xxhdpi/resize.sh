#!/bin/sh

if (( $# != 1 ))
then
    echo "Wrong number of arguments supplied. Usage: ./resize [filename]"
else
	if [ -f $1 ]
		then
			sips -Z $1 
		else
			echo "file not found"
	fi
fi
