import requests
import sys
import re
import os


def send_submission(ass_name, file_names, class_name='cse2010'):
    """Submission code should be stored as an environment var"""

    if not ass_name or not file_names:
        sys.exit('Invalid inputs')

    # Confirm inputs
    print ''
    print '  Class: ', class_name
    print 'Asgnmnt: ', ass_name
    print '  Files:'
    for f_name in file_names:
        print '\t', f_name
    print ''

    confirm = raw_input('CONFIRM? [y/n]')
    if not confirm == 'y':
        print 'ABORTED'
        sys.exit(0)

    form = {
        'controlCode': os.environ['...'],
        'class': class_name,
        'assignment': ass_name
    }

    files = []
    for fn in file_names:
        files.append(('files[]', open(fn,'rb')))

    response = requests.post('...', data=form, files=files)

    # Print response
    toggle = False
    for l in response.text.splitlines():
        if toggle:
            print l
            break
        if re.match(r'<div id="submitMsg" class="well">', l):
            toggle = True


if len(sys.argv) < 3:
    print """Usage:
    python submit.py    Print this message

    python submit.py 1 Lab4-Lists/src/file.java another_file.java
                     ^ part number    ^ path to the files

    python submit.py -i file1.java file2.java
                      ^ manually enter assignment and class name
"""

elif sys.argv[1] == '-i':
    file_names = sys.argv[2:]
    ass_name = raw_input('Assignment Name: ')
    class_name = raw_input('Class Name: ')
    send_submission(ass_name, file_names, class_name)

else:
    lab_part = sys.argv[1]
    file_names = sys.argv[2:]

    # Parse lab name
    lab_num_len = 1
    if not file_names[0][4] == '-':
        lab_num_len = 2
    lab_num = file_names[0][3:3+lab_num_len]
    lab_name = 'lab' + lab_num + '_' + lab_part

    send_submission(lab_name, file_names)
