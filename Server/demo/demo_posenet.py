import sys
import os
import os.path as osp
import argparse
import numpy as np
import cv2
import torch
import torchvision.transforms as transforms
from torch.nn.parallel.data_parallel import DataParallel
import torch.backends.cudnn as cudnn
import math
import socket


sys.path.insert(0, osp.join('..', 'main'))
sys.path.insert(0, osp.join('..', 'data'))
sys.path.insert(0, osp.join('..', 'common'))

from main.config_posenet import cfg
from main.model_posenet import get_pose_net
from data.dataset_posenet import generate_patch_image
from common.utils.pose_utils_posenet import process_bbox, pixel2cam
from common.utils.vis_posenet import vis_keypoints, vis_3d_multiple_skeleton

from select import select
import sys

posenet_return = ''
height = 0
fallen_check = ''


# distance from point1(x1, y1, z1) to point2(x2 ,y2, z2)
def cal_distance(x1, y1, z1 , x2, y2, z2):

    dis = math.sqrt(pow(x2-x1,2) + pow(y2-y1,2) + pow(z2-z1,2))
    return dis

# length of height
def cal_height(pose_3d):

    # head to nose
    head_to_nose = cal_distance(pose_3d[10][0],pose_3d[10][1],pose_3d[10][2],pose_3d[9][0],pose_3d[9][1],pose_3d[9][2])
    # nose to neck
    nose_to_neck = cal_distance(pose_3d[9][0],pose_3d[9][1],pose_3d[9][2],pose_3d[8][0],pose_3d[8][1],pose_3d[8][2])
    # neck to torso
    neck_to_torso = cal_distance(pose_3d[8][0], pose_3d[8][1], pose_3d[8][2], pose_3d[7][0], pose_3d[7][1], pose_3d[7][2])
    # torso to pelvis
    torso_to_pelvis = cal_distance(pose_3d[7][0], pose_3d[7][1], pose_3d[7][2], pose_3d[0][0], pose_3d[0][1], pose_3d[0][2])
    # Rhip to Rknee
    Rhip_to_Rknee = cal_distance(pose_3d[1][0], pose_3d[1][1], pose_3d[1][2], pose_3d[2][0], pose_3d[2][1], pose_3d[2][2])
    # Rknee to Rankle
    Rknee_to_Rankle = cal_distance(pose_3d[2][0], pose_3d[2][1], pose_3d[2][2], pose_3d[3][0], pose_3d[3][1], pose_3d[3][2])

    #print(head_to_nose, nose_to_neck, neck_to_torso, torso_to_pelvis, Rhip_to_Rknee, Rknee_to_Rankle)
    total = head_to_nose + nose_to_neck + neck_to_torso + torso_to_pelvis + Rhip_to_Rknee + Rknee_to_Rankle

    return total

# detect fall
# y = ax + b -> bridge
def check_fall(r_ankle_x, r_ankle_y, l_ankle_x, l_ankle_y, a, b) :

    if r_ankle_y > (a*r_ankle_x + b):
        return "fallen"
    elif l_ankle_y > (a*l_ankle_x + b):
        return "fallen"
    else :
        return "non-fallen"

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--gpu', type=str, default= "0" ,dest='gpu_ids')
    parser.add_argument('--test_epoch', type=str, default="24", dest='test_epoch')
    args = parser.parse_args()

    # test gpus
    if not args.gpu_ids:
        assert 0, print("Please set proper gpu ids")

    if '-' in args.gpu_ids:
        gpus = args.gpu_ids.split('-')
        gpus[0] = 0 if not gpus[0].isdigit() else int(gpus[0])
        #gpus[1] = len(mem_info()) if not gpus[1].isdigit() else int(gpus[1]) + 1
        args.gpu_ids = ','.join(map(lambda x: str(x), list(range(*gpus))))

    assert args.test_epoch, 'Test epoch is required.'
    return args

def pose_demo(bbox, root_depth, img_array) :

    global fallen_check
    global posenet_return
    global height

    # argument parsing
    args = parse_args()
    cfg.set_args(args.gpu_ids)
    cudnn.benchmark = True

    # MuCo joint set
    # joint_num = 21
    # joints_name = ('Head_top', 'Thorax', 'R_Shoulder', 'R_Elbow', 'R_Wrist', 'L_Shoulder', 'L_Elbow', 'L_Wrist', 'R_Hip', 'R_Knee', 'R_Ankle', 'L_Hip', 'L_Knee', 'L_Ankle', 'Pelvis', 'Spine', 'Head', 'R_Hand', 'L_Hand', 'R_Toe', 'L_Toe')
    # flip_pairs = ( (2, 5), (3, 6), (4, 7), (8, 11), (9, 12), (10, 13), (17, 18), (19, 20) )
    # skeleton = ( (0, 16), (16, 1), (1, 15), (15, 14), (14, 8), (14, 11), (8, 9), (9, 10), (10, 19), (11, 12), (12, 13), (13, 20), (1, 2), (2, 3), (3, 4), (4, 17), (1, 5), (5, 6), (6, 7), (7, 18) )

    # Human36M
    joint_num = 18  # original:17, but manually added 'Thorax'
    joints_name = (
        'Pelvis', 'R_Hip', 'R_Knee', 'R_Ankle', 'L_Hip', 'L_Knee', 'L_Ankle', 'Torso', 'Neck', 'Nose', 'Head',
        'L_Shoulder',
        'L_Elbow', 'L_Wrist', 'R_Shoulder', 'R_Elbow', 'R_Wrist', 'Thorax')
    flip_pairs = ((1, 4), (2, 5), (3, 6), (14, 11), (15, 12), (16, 13))
    skeleton = (
        (0, 7), (7, 8), (8, 9), (9, 10), (8, 11), (11, 12), (12, 13), (8, 14), (14, 15), (15, 16), (0, 1), (1, 2),
        (2, 3),
        (0, 4), (4, 5), (5, 6))

    # for ske in skeleton:
    #    print("{} {}".format(joints_name[ske[0]],joints_name[ske[1]]))

    # snapshot load
    model_path = './snapshot_%d.pth.tar' % int(args.test_epoch)
    assert osp.exists(model_path), 'Cannot find model at ' + model_path
    #print('Load checkpoint from {}'.format(model_path))

    model = get_pose_net(cfg, False, joint_num)
    model = DataParallel(model).cuda()
    ckpt = torch.load(model_path)

    model.load_state_dict(ckpt['network'])
    model.eval()

    # prepare input image
    transform = transforms.Compose(
        [transforms.ToTensor(), transforms.Normalize(mean=cfg.pixel_mean, std=cfg.pixel_std)])
    #img_path = 'received_image.jpg'
    #img_path = 'input.jpg'
    #original_img = cv2.imread(img_path)
    original_img = img_array
    original_img_height, original_img_width = original_img.shape[:2]

    #
    # prepare bbox
    #bbox_list = [
    #    [139.41, 102.25, 222.39, 241.57], \
    #    [287.17, 61.52, 74.88, 165.61], \
    #    [540.04, 48.81, 99.96, 223.36], \
    #    [372.58, 170.84, 266.63, 217.19], \
    #   [0.5, 43.74, 90.1, 220.09]]  # xmin, ymin, width, height

    bbox_list = bbox

    # obtain this from RootNet (https://github.com/mks0601/3DMPPE_ROOTNET_RELEASE/tree/master/demo)
    # change type to list
    root_depth_list = [root_depth]
    assert len(bbox_list) == len(root_depth_list)
    person_num = len(bbox_list)

    # normalized camera intrinsics
    focal = [1500, 1500]  # x-axis, y-axis
    princpt = [original_img_width / 2, original_img_height / 2]  # x-axis, y-axis
    #print('focal length: (' + str(focal[0]) + ', ' + str(focal[1]) + ')')
    #print('principal points: (' + str(princpt[0]) + ', ' + str(princpt[1]) + ')')

    # for each cropped and resized human image, forward it to PoseNet
    output_pose_2d_list = []
    output_pose_3d_list = []

    # define person_num only 1
    person_num = 1

    for n in range(person_num):
        bbox = process_bbox(np.array(bbox_list[n]), original_img_width, original_img_height)

        # patch -> cutten image by person
        img, img2bb_trans = generate_patch_image(original_img, bbox, False, 1.0, 0.0, False)
        # cude() -> upload to gpu
        img = transform(img).cuda()[None, :, :, :]

        # forward
        # no calculate gradient decent
        with torch.no_grad():
            pose_3d = model(img)  # x,y: pixel, z: root-relative depth (mm)

        # inverse affine transform (restore the crop and resize)
        # numpy is calculated in cpu only
        pose_3d = pose_3d[0].cpu().numpy()
        # we have to study camera parmater, output_shape, input_shape -> maybe this will be changed when changing image
        pose_3d[:, 0] = pose_3d[:, 0] / cfg.output_shape[1] * cfg.input_shape[1]
        pose_3d[:, 1] = pose_3d[:, 1] / cfg.output_shape[0] * cfg.input_shape[0]
        pose_3d_xy1 = np.concatenate((pose_3d[:, :2], np.ones_like(pose_3d[:, :1])), 1)
        img2bb_trans_001 = np.concatenate((img2bb_trans, np.array([0, 0, 1]).reshape(1, 3)))
        pose_3d[:, :2] = np.dot(np.linalg.inv(img2bb_trans_001), pose_3d_xy1.transpose(1, 0)).transpose(1, 0)[:, :2]
        output_pose_2d_list.append(pose_3d[:, :2].copy())

        # root-relative discretized depth -> absolute continuous depth
        pose_3d[:, 2] = (pose_3d[:, 2] / cfg.depth_dim * 2 - 1) * (cfg.bbox_3d_shape[0] / 2) + root_depth_list[n]
        pose_3d = pixel2cam(pose_3d, focal, princpt)

        # for i in range(len(pose_3d)):
        #    print("joint : " + joints_name[i] , pose_3d[i])

        # print(n, "person's height : ", cal_height(pose_3d), "mm")
        height = str(round(cal_height(pose_3d) * 0.1, 2))

        # print(pose_3d)
        # print(len(pose_3d))

        output_pose_3d_list.append(pose_3d.copy())

    # visualize 2d poses
        vis_img = original_img.copy()

        # start_point & end_point
        point_1 = (177, 0)
        point_2 = (38, 416)

        # y = ax + b
        a = point_2[1] - point_1[1] / point_2[0] - point_1[0]
        b = point_1[1] - (a * point_1[0])

        # draw a line(bridge)
        cv2.line(vis_img, point_1, point_2, (0, 0, 255), 5)

        for n in range(person_num):

            vis_kps = np.zeros((3,joint_num))
            vis_kps[0,:] = output_pose_2d_list[n][:,0]
            vis_kps[1,:] = output_pose_2d_list[n][:,1]

            print("y = ",a,"x + ",b)
            # check fallen state
            print(check_fall(vis_kps[0,3],vis_kps[1,3],vis_kps[0,6],vis_kps[1,6],a,b))
            fallen_check = check_fall(vis_kps[0,3],vis_kps[1,3],vis_kps[0,6],vis_kps[1,6],a,b)

            #print("pelvis : (", vis_kps[0, 0], ",", vis_kps[1, 0], ")")
            #print("right knee : (", vis_kps[0, 2], ",", vis_kps[1, 2], ")")
            #print("left knee : (", vis_kps[0, 5], ",", vis_kps[1, 5], ")")
            print("right ankle : (", vis_kps[0,3] , "," , vis_kps[1,3] , ")")
            print("left ankle : (", vis_kps[0,6] , "," , vis_kps[1,6] , ")")
            vis_kps[2,:] = 1
            vis_img = vis_keypoints(vis_img, vis_kps, skeleton)

        #cv2.imshow('output_pose_2d',vis_img)
        cv2.imwrite('output_pose_2d.jpg', vis_img)

    # visualize 3d poses
    #vis_kps = np.array(output_pose_3d_list)
    #vis_3d_multiple_skeleton(vis_kps, np.ones_like(vis_kps), skeleton, 'output_pose_3d (x,y,z: camera-centered. mm.)')

    print(height + "cm")

    posenet_return = height

    return posenet_return






