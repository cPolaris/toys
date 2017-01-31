import socket
import SocketServer



class GreetingTcpServer(SocketServer.TCPServer):
    pass


class GreetingReqeustHandler(SocketServer.BaseRequestHandler):
    def handle(self):



def main():
    server = GreetingTcpServer(('', 23333), )


if __name__ == '__main__':
    main()