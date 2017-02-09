from datetime import datetime
import subprocess
import requests
import json
import re


COMMAND = ['fail2ban-client', 'status', 'ufw-port-scan']
API_URL = 'https://api.github.com/gists/'
GIST_ID = '000'
USER_NAME = '000'
PAT = '000'
LOG_FILE_NAME = '000'
now = datetime.now()
now_str = str(now.year) + '-' + str(now.month) + '-' + str(now.day) + ' ' + str(now.hour) + ':' + str(now.minute)

# Execute command and process output
proc = subprocess.Popen(COMMAND, stdout=subprocess.PIPE)
lines = proc.stdout.read().decode().split('\n')

bannums = []

for l in lines:
	if not re.search(r'Total', l) is None:
		bannums.append(l.split('\t')[1])

log_line = now_str + ',' + str(bannums[0]) + ',' + str(bannums[1]) + '\n'

# write_path = sys.argv[1]
# with open(write_path, 'a') as f:
#	f.write(log_line)

# Get old content
gist_url = API_URL + GIST_ID
response = json.loads(requests.get(gist_url).text)

# Post new content
updated_content = response['files'][LOG_FILE_NAME]['content'] + log_line
data_to_post = {'files': {LOG_FILE_NAME: {'content': updated_content}}}
patch_response = requests.patch(gist_url, data=json.dumps(data_to_post), auth=(USER_NAME, PAT))
print(patch_response.text)
