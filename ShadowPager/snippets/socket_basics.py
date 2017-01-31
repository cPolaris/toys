import socket
import sys

SOCK_NAME='volcano'
ADDR='127.0.0.1'
PORT=52341
MESSAGE='\r\nComputer science if no more about computers than astronomy is about telescopes. - Edsger Dijkstra\r\n'

def print_sock(sock):
	print('family: ' + repr(sock.family) +
		  ' type: ' + repr(sock.type) +
		  ' proto: ' + repr(sock.proto))

def recv_unix():
	with socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM) as sock:
		print_sock(sock)
		sock.bind(SOCK_NAME)
		sock.settimeout(8)
		msg = sock.recv(1024)
		print('got: ' + msg.decode('UTF-8'))


def recv_inet():
	with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
		print_sock(sock)
		sock.bind((ADDR, PORT))
		msg = sock.recv(1024)
		print('got:' + msg.decode('UTF-8'))


def send_unix():
	with socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM) as sock:
		print_sock(sock)
		sock.connect(SOCK_NAME)
		msg = sock.sendall(MESSAGE.encode('UTF-8'))
		print('sent')


def send_inet():
	with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
		print_sock(sock)
		sock.connect((ADDR, PORT))
		msg = sock.sendall(MESSAGE.encode('UTF-8'))
		print('sent')


if __name__ == '__main__':
	if sys.argv[1] == 'ru':
		recv_unix()
	elif sys.argv[1] == 'su':
		send_unix()
	elif sys.argv[1] == 'ri':
		recv_inet()
	elif sys.argv[1] == 'si':
		send_inet()
