import time
from http.server import BaseHTTPRequestHandler, HTTPServer
HOST_NAME = 'localhost'
PORT_NUMBER = 4657

class MyHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        paths = {
            '/': {'status': 200}
        }

        if self.path in paths:
            self.respond(paths[self.path])
        else:
            self.respond({'status': 500})
    def handle_http(self, status_code, path):
        print("\n----- Request Start ----->\n")
        print(self.path)
        print(self.headers)
        print("<----- Request End -----\n")
        self.send_response(status_code)
        self.send_header('Content-type', 'text/html')
        self.send_header('Set-Cookie', 'foo=23')
        self.end_headers()
        content = 'choochooo'
        return bytes(content, 'UTF-8')
    def respond(self, opts):
        response = self.handle_http(opts['status'], self.path)
        self.wfile.write(response)

if __name__ == '__main__':
    httpd = HTTPServer((HOST_NAME, PORT_NUMBER), MyHandler)
    print(time.asctime(), 'Server Starts - %s:%s' % (HOST_NAME, PORT_NUMBER))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.server_close()
    print(time.asctime(), 'Server Stops - %s:%s' % (HOST_NAME, PORT_NUMBER))
