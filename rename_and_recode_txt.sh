# Have a file tree that stores tons of txt files
# all of which follows the pattern of "[numbering]《filename》.txt"
#
# Want to preserve only filename, also change the encoding to UTF-8
# so that I can actually type out the filename, and view the content correctly
#
# Some of the file name even contains space, which I will have to fix manually
# The time I spent working on this script is roughly 2-3 times the time doing
# it all by hand. Totally worth it.

FILES=$(find . -name '*.txt')
for f in $FILES
do
    f=${f:2}          # Remove the './' that find has added to the path. Because I'm OCD
    path=${f%/*}/     # Path expands to the first / from the end
    new_name=${f##*/}
    #               strip [xx]     delete useless symbols      translate to ascii symbols
    new_name=$(echo $new_name | tr -d '[]《》〈〉“”，·' | tr -s '——' '-' | tr '：（）、！' '_')
    # echo $new_name
    iconv -f GBK -t UTF-8 $f > $path$new_name  # change encoding
    rm $f
done
