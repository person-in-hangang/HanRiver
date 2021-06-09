import socket
import os
import argparse
import threading
import time
import numpy as np
import cv2
import datetime
from demo.demo_rootnet import root_demo

from _thread import *
import base64
host = ''  # all interface
port = 7002 #
t1 = None
t2 = None
t3 = None
t4 = None
t5 = None
t6 = None
t7 = None

bbox = ''
x = ''
y = ''

def thread1(client_socket, addr):
    print("111")

    global t1, t2, t3

    global bbox

    print("detection camera : ", addr)
    size = bytearray()

    while True:
        try: #image
            temp = client_socket.recv(1)

        except KeyboardInterrupt:
            break
        # print("this22")
        # print(type(temp))
        # print(temp)
        #recive image size
        if temp.decode(errors='ignore') == '@':
            break
        size = size + temp

    image = bytearray()

    #recive image - size
    for i in range(int.from_bytes(size, byteorder='big')):
        image = image + client_socket.recv(1)

    #temp3 = base64.b64encode(image)
    #print("btebyte")
    #print(temp3)
    #t2.send(temp3)

    filename = "/home/chearsoon/Desktop/3DMPPE_POSENET_RELEASE-master/demo/image.jpg"
    f = open(filename, 'wb')
    f.write(image)
    print("접 입니다. : ")
    f.close()

    # split received bbox string
    received_str = str(bbox)
    temp = received_str.split('@')
    x_min, y_min, width, height = int(temp[1]), int(temp[2]), int(temp[3]), int(temp[4])
    # received bbox
    bbox_split = [[x_min, y_min, width, height]]

    image = bytes(image)
    encoded_img = np.fromstring(image, dtype=np.byte)
    img = cv2.imdecode(encoded_img, cv2.IMREAD_COLOR)

    person_height = root_demo(bbox_split, img)

    # print(person_height)

    now = datetime.datetime.now()
    now = str(now)
    # print(now)

    time_height = now + ',' + person_height
    print(time_height)
    time_height = time_height.encode()
    length = len(time_height)

    #person_height = bytearray(person_height)

    if t2 is not None:
        t2.send(image)
        t2.close()
        t2 = None

    if t3 is not None:
        t3.send(length.to_bytes(4,byteorder="little"))
        t3.send(time_height)
        print("send finish_person_height")
        t3.close()
        t3 = None

    t1.close()
    t1 = None

    """
    file = open(filename,'rb')
    data = file.read()
    print(type(data))
    print(data)
    t2.send(data)
    f.close()
    """
def thread2(client_socket, addr):
    print("222")
    # print("접속한 2 주소 입니다. : ", addr)

    #while True:
    #    client_socket.recv(1024)
    #    break
    """
    while True:
        if t1 is not None and t1.fileno()!=-1:
            string = "fallfallfall"
            client_socket.sendall(string.encode())
            break
            # client_socket.close()
    """
def thread3(client_socket, addr):
    aaa =addr
    print("3333")
    #print("접속한 3 주소 입니다. : ", addr)

# get tracking coord from camera 2
def thread4(client_socket, addr):

    global t4, t5
    print("44")

    print("tracking camera : ", addr)

    global x, y

    camera2_coordinates = x + ',' + y
    print(camera2_coordinates)
    print(type(camera2_coordinates))
    # print("sdf")
    camera2_coordinates = camera2_coordinates.encode()
    length = len(camera2_coordinates)

    if t5 is not None:
        print("asdlkfnsdklfnkldsnfklsd : ", length.to_bytes(4, byteorder="little"))
        print("asdlkfnsdklfnkldsnfklsd : ", camera2_coordinates)
        t5.send(length.to_bytes(4, byteorder="little"))
        t5.send(camera2_coordinates)
        print("camera2 coordinates : ", camera2_coordinates)
        t5.close()
        t5 = None
        print("send finish_coordinates")

    t4.close()
    t4 = None

# total application's function that receive data from camera2
def thread5(client_socket, addr):
    ad3d = addr
    print("5555")

    # print("접속한 5 주소 입니다. : ", addr)

# get the tracking image from camera2
def thread6(client_socket, addr):
    print("6666")

    global t6, t7

    # print("접속한 6 주소 입니다. : ", addr)

    size = bytearray()

    while True:
        try:  # image
            temp = client_socket.recv(1)

        except KeyboardInterrupt:
            break
        # print("this22")
        # print(type(temp))
        # print(temp)
        # recive image size
        if temp.decode(errors='ignore') == '@':
            break
        size = size + temp

    image = bytearray()

    # recive image - size
    for i in range(int.from_bytes(size, byteorder='big')):
        image = image + client_socket.recv(1)

    # temp3 = base64.b64encode(image)
    # print("btebyte")
    # print(temp3)
    # t2.send(temp3)

    print(image)

    filename = "/home/chearsoon/Desktop/3DMPPE_POSENET_RELEASE-master/demo/camera2_image.jpg"
    f = open(filename, 'wb')
    f.write(image)
    # print("접 입니다. : ")
    f.close()

    image = bytes(image)

    print("sdfsdfsdfsdfsdfsdfsdfsdfsd")
    #
    if t7 is not None:
        t7.send(image)
        t7.close()
        t7 = None
    #
    t6.close()
    t6 = None

# at total application to get tracking image
def thread7(client_socket, addr):
    ad2d = addr
    print("777")
    # print("접속한 7 주소 입니다. : ", addr)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((host, port))

server_socket.listen()

while True:
    try:
        client_socket, addr = server_socket.accept()
    except KeyboardInterrupt:
        server_socket.close()
        print("Keyboard interrupt")

    print("핸들러")
    try:
        temp1 = client_socket.recv(1024)
    except KeyboardInterrupt:
        print("error")
    # print(temp1.decode("utf-8"), len(temp1))
    fullStr = str(temp1)
    splitt = fullStr.split(':')
    front,back = int(splitt[1]),splitt[2]

    if front == 1:
        # print("camera 1")
        bbox = back

        t1 = client_socket
        start_new_thread(thread1, ((client_socket, addr)))

    elif front == 2:
        # print("camera1_image")
        t2 = client_socket
        start_new_thread(thread2, ((client_socket, addr)))



    elif front == 3:
        # print("camera1_height")
        # print(back)
        t3 = client_socket
        start_new_thread(thread3, ((client_socket, addr)))
    elif front == 4:
        # print("camera2")
        # print(back)

        coor = back
        coor = coor.split(',')
        x = coor[0]
        y = coor[1]
        y = y[:-1]

        t4 = client_socket
        start_new_thread(thread4, ((client_socket, addr)))

    elif front == 5:
        # print("camera2_coordinates")
        # print(back)
        t5 = client_socket
        print("t5 start")
        start_new_thread(thread5, ((client_socket, addr)))

    elif front == 6:
        # print("camera2 image")
        # print(back)
        t6 = client_socket
        start_new_thread(thread6, ((client_socket, addr)))

    elif front == 7:
        # print("at notice tracking image")
        # print(back)
        t7 = client_socket
        start_new_thread(thread7, ((client_socket, addr)))


    if t1 is not None and t2 is not None and t3 is not None and t4 is not None and t5 is not None and t6 is not None and t7 is not None:
        while t1.fileno() == -1 & t2.fileno() == -1 & t3.fileno()==-1:
            server_socket.close()
            exit()