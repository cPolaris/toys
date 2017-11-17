#!/usr/local/bin/python3
import sys
import re

def dashify(s):
    return '-'.join(re.findall(r'[a-z0-9]+', s.strip().lower()))

if __name__ == '__main__':
    # print(dashify('roses - are_ red'))
    # print(dashify('Hello World And-Foo bar'))
    # print(dashify('cRuncY-peanUT BUTTER'))
    # print(dashify('The Machine-Learning Algorithm as Creative Musical Tool'))
    print(dashify(sys.argv[1]))
