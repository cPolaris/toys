FILENAME='/usr/share/dict/web2'

def normalize(word):
    lw = list(word)
    lw.sort()
    return ''.join(lw)

def next_word():
    with open(FILENAME, 'r') as fin:
        for word in fin:
            yield word.strip().lower()

anas = {}
nextword = next_word()

for word in nextword:
    normal = normalize(word)
    if normal in anas:
        anas[normal].append(word)
    else:
        anas[normal] = [word]

for l in anas.values():
    if len(l) > 1:
        print(' '.join(l))
