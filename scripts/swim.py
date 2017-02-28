# I have a file tree...
# .
# ├── d1
# │   ├── d11
# │   │   └── foo11
# │   └── foo1
# ├── d2
# ├── d3
# │   └── d33
# │       └── d333
# │           └── foo333
# ├── foo2
# └── foo3
# I'd like all the files to be moved to the root directory. And may as well
# remove all the empty directories.
# So that it looks like:
# .
# ├── foo1
# ├── foo11
# ├── foo2
# ├── foo3
# ├── foo333
# └── swim.py
# I call this operation 'swim the files' because they are all
# raised to the 'surface' level of the tree
import os

BASE = os.path.abspath('.')

def do_dir(curr_path, prefix):
    print(prefix + 'Doing ' + os.path.relpath(curr_path, BASE))
    for f in os.listdir(curr_path):
        fp = os.path.join(curr_path, f)
        if os.path.isdir(fp):
            do_dir(fp, prefix + '\t')
        elif os.path.isfile(fp):
            top = os.path.join(BASE, f)
            print(prefix + 'Moving ' + fp + ' -> ' + top)
            os.rename(fp, top)
    print(prefix + 'Removing directory ' + os.path.relpath(curr_path, BASE))
    os.rmdir(curr_path)

if __name__ == '__main__':
    for f in os.listdir():
        if os.path.isdir(f):
            do_dir(os.path.abspath(f), '')
