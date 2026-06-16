/*
 Navicat Premium Dump SQL

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : study_main

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 07/01/2026 22:21:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for db_account
-- ----------------------------
DROP TABLE IF EXISTS `db_account`;
CREATE TABLE `db_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `register_time` datetime DEFAULT NULL,
  `mute` tinyint DEFAULT '0',
  `banned` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_account
-- ----------------------------
BEGIN;
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `avatar`, `register_time`, `mute`, `banned`) VALUES (1, 'test', '$2a$10$FVnhxXODi7K0GjBpjKEdPuLUpRswYmeW8XR0zbYT3vhVmKn20HIIK', '404213506@qq.com', 'admin', '/avatar/5d0b74e158f3458a8a2871ac87485c53', '2023-08-27 00:18:20', 0, 0);
INSERT INTO `db_account` (`id`, `username`, `password`, `email`, `role`, `avatar`, `register_time`, `mute`, `banned`) VALUES (2, 'user', '$2a$10$i6NI8UtwqJb/LwcG.kj8..QQI510nYcAaGqpwBnajRl3vQeag8DFi', 'nagocoler@qq.com', 'user', NULL, '2023-10-28 18:22:18', 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for db_account_details
-- ----------------------------
DROP TABLE IF EXISTS `db_account_details`;
CREATE TABLE `db_account_details` (
  `id` int NOT NULL,
  `gender` tinyint DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `qq` varchar(255) DEFAULT NULL,
  `wx` varchar(255) DEFAULT NULL,
  `desc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_account_details
-- ----------------------------
BEGIN;
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (1, 0, '26371286731', '213213213', 'asdasd', '撒的谎金阿奎稍等哈极客飒打卡机阿萨达哈卡四大三抗打击');
INSERT INTO `db_account_details` (`id`, `gender`, `phone`, `qq`, `wx`, `desc`) VALUES (2, 1, '21323213111', '12321321', 'sdasdsad', 'sadasdasda撒大大是的撒的案底啊是');
COMMIT;

-- ----------------------------
-- Table structure for db_account_privacy
-- ----------------------------
DROP TABLE IF EXISTS `db_account_privacy`;
CREATE TABLE `db_account_privacy` (
  `id` int NOT NULL,
  `phone` tinyint DEFAULT NULL,
  `email` tinyint DEFAULT NULL,
  `wx` tinyint DEFAULT NULL,
  `qq` tinyint DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_account_privacy
-- ----------------------------
BEGIN;
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (1, 0, 1, 0, 1, 1);
INSERT INTO `db_account_privacy` (`id`, `phone`, `email`, `wx`, `qq`, `gender`) VALUES (2, 1, 1, 1, 1, 1);
COMMIT;

-- ----------------------------
-- Table structure for db_email_record
-- ----------------------------
DROP TABLE IF EXISTS `db_email_record`;
CREATE TABLE `db_email_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_email_record
-- ----------------------------
BEGIN;
INSERT INTO `db_email_record` (`id`, `email`, `title`, `content`, `time`, `status`) VALUES (1, 'nagocoler@qq.com', '您的邮件修改验证邮件', '您好，您正在绑定新的电子邮件地址，验证码: 441786，有效时间3分钟，如非本人操作，请无视。', '2025-12-25 19:09:49', 0);
INSERT INTO `db_email_record` (`id`, `email`, `title`, `content`, `time`, `status`) VALUES (2, 'nagocoler@qq.com', '您的邮件修改验证邮件', '您好，您正在绑定新的电子邮件地址，验证码: 625725，有效时间3分钟，如非本人操作，请无视。', '2025-12-25 19:11:05', 1);
INSERT INTO `db_email_record` (`id`, `email`, `title`, `content`, `time`, `status`) VALUES (3, 'nagocoler@qq.com', '您的邮件修改验证邮件', '您好，您正在绑定新的电子邮件地址，验证码: 976769，有效时间3分钟，如非本人操作，请无视。', '2025-12-25 19:11:51', 1);
INSERT INTO `db_email_record` (`id`, `email`, `title`, `content`, `time`, `status`) VALUES (4, 'nagocoler@qq.com', '您的邮件修改验证邮件', '您好，您正在绑定新的电子邮件地址，验证码: 598486，有效时间3分钟，如非本人操作，请无视。', '2025-12-25 19:15:09', 2);
COMMIT;

-- ----------------------------
-- Table structure for db_image_store
-- ----------------------------
DROP TABLE IF EXISTS `db_image_store`;
CREATE TABLE `db_image_store` (
  `uid` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_image_store
-- ----------------------------
BEGIN;
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/11a7f457ce4e428baa20c591d70e8e22', '2023-10-09 18:59:07');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/1d22faf648524fa584141e0cc7b32e57', '2023-10-09 19:00:00');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/ab6864c921b946f590b02baf8f89c4de', '2023-10-09 19:01:32');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/9c874fe3fc914efab1244104f702292f', '2023-10-09 19:01:33');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/fd773f32e588465ebde7a7b0fd00e31a', '2023-10-09 19:01:34');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/4036068090c0454c8bcde6a6b683c0bd', '2023-10-09 19:01:35');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/9c64875805e4427ab9cc7fa4b3096196', '2023-10-09 19:01:35');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/1912653ba1954ac8b025c3da463022f1', '2023-10-09 19:01:36');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/bfdd1074455d4ac8b540208003bf4515', '2023-10-09 19:01:37');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/5b0ff4e3fea0463dacd8cd520fd9d6f0', '2023-10-09 19:02:11');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/bd7b40e47df8463c83c96378ee35103a', '2023-10-09 19:02:13');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/a458e2224e5249a69eb4952226845cd9', '2023-10-09 19:02:15');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/0da13dc9aa4b45e4a167fb299e7c9f07', '2023-10-09 19:02:17');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/96c72d0fc0404300a33691ffa1a86e3a', '2023-10-09 19:02:21');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/a9a727c5325c46279f4b1ea4db4c8128', '2023-10-09 22:09:46');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/38816cca313a4da28030114c11c52c6a', '2023-10-09 22:10:15');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231009/db5f650412534900b297683af475f2cb', '2023-10-09 22:41:12');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231013/9f1057337d1b4f368d8c307f850922aa', '2023-10-13 17:25:13');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231013/c56599fb7cbe4d7db3d51d12198e7390', '2023-10-13 17:25:29');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231013/c636248526d94c0d8c026a027b6b15d1', '2023-10-13 18:36:07');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231013/6f527b374d0245eca5d16ded78cdfafd', '2023-10-13 18:37:24');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231013/66ce6817e073414396697c4fe538d8b7', '2023-10-13 18:37:54');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231023/c292300bb3bc4adcb71fe88e13fea9c1', '2023-10-23 16:54:41');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231023/fca00bb202d7425fa60ce0eac2d7ee34', '2023-10-23 16:54:58');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231023/6c95e362dbfe4b11b3b89a45f07bbd83', '2023-10-23 19:14:42');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231023/85903aad7db046a38d78ac5eb4714504', '2023-10-23 19:14:56');
INSERT INTO `db_image_store` (`uid`, `name`, `time`) VALUES (1, '/cache/20231028/8c4d5883582a4c57b82e23ab1628c41f', '2023-10-28 15:45:31');
COMMIT;

-- ----------------------------
-- Table structure for db_notification
-- ----------------------------
DROP TABLE IF EXISTS `db_notification`;
CREATE TABLE `db_notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_notification
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for db_topic
-- ----------------------------
DROP TABLE IF EXISTS `db_topic`;
CREATE TABLE `db_topic` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `intro` text,
  `content` text,
  `uid` int DEFAULT NULL,
  `type` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `top` tinyint DEFAULT '0',
  `locked` tinyint DEFAULT '0',
  `invisible` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_topic
-- ----------------------------
BEGIN;
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (4, '你们Navicat是不是在用盗版?', '千万别用了，小心数据库被勒索！\n', '{\"ops\":[{\"insert\":\"千万别用了，小心数据库被勒索！\\n\"}]}', 1, 1, '2023-10-13 18:23:49', 1, 0, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (6, '论坛出现超大BUG赶紧修', '为什么不能直接给我匹配一个女友，要同城的，最好同学校\n', '{\"ops\":[{\"insert\":\"为什么不能直接给我匹配一个女友，要同城的，最好同学校\\n\"}]}', 1, 3, '2023-10-13 18:28:30', 0, 0, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (9, '测试我是测试帖子', NULL, '{\"ops\":[{\"insert\":\"测试一下缓存是否生效\\n\"}]}', 1, 2, '2023-10-13 19:35:43', 0, 1, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (10, '撒电话卡进度哈健康四大会计说', NULL, '{\"ops\":[{\"insert\":\"大大叔大婶肯定撒户籍大刷卡机打撒科技大\\n\"}]}', 1, 1, '2023-10-13 19:49:27', 0, 0, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (15, '请问项目的更新速度能不能再快一点', '太少了不够看啊，能不能再更新快一点！！！！！\n\n代码写多一点，我好拿来当毕设\n\n会撒娇大厦尽快的哈手机卡打撒\n', '{\"ops\":[{\"insert\":\"太少了不够看啊，能不能再更新快一点！！！！！\\n\"},{\"attributes\":{\"width\":\"321\"},\"insert\":{\"image\":\"http://localhost:8080/images/cache/20231023/c292300bb3bc4adcb71fe88e13fea9c1\"}},{\"insert\":\"\\n代码写多一点，我好拿来当毕设\\n\"},{\"attributes\":{\"width\":\"344\"},\"insert\":{\"image\":\"http://localhost:8080/images/cache/20231023/fca00bb202d7425fa60ce0eac2d7ee34\"}},{\"insert\":\"\\n会撒娇大厦尽快的哈手机卡打撒\\n\"}]}', 1, 3, '2023-10-23 16:55:02', 0, 0, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (16, '兄妹喝奶奶捡回的未开封饮料被送医抢救，饮料检出三甲基硅醇', '一个多月前，深圳宝安，6岁的哥哥和4岁妹妹喝了奶奶从外面垃圾桶捡回来的未开封的饮料，随即出现呕吐、腹痛症状，被送医抢救。\n\n4岁1个月大的中毒女孩病情严重\n10月22日，兄妹俩住院治疗38天，表姑妈黎女士告诉华商报大风新闻记者，“兄妹俩当时分着喝了一小部分，就上吐下泻，后来确定孩子喝了这个饮料中毒，里面检出了三甲基硅醇。”\n奶奶捡回饮料爷爷帮忙给打开\n兄妹俩只喝了一点就上吐下泻\n10月22日，表姑妈黎女士接受华商报大风新闻记者采访表示，6岁的表侄子和4岁的表侄女在深圳儿童医院住院抢救已经38天。\n黎女士介绍，事发9月16日，孩子60来岁的奶奶拾废品，捡回来一瓶未过期的饮料。\n“因为捡回来的饮料瓶子根本没有开封，是一个完好无损的饮料瓶，孩子自己都打不开，是孩子爷爷帮忙打开给他们喝的。倒出来只喝了一点，两个孩子都翻了，上吐下泻，兄妹俩应该喝的量是差不多，因为孩子一喝就感觉味道不对，兄妹俩分着喝了一会儿就不行了。”\n黎女士解释称，虽然是孩子奶奶把饮料捡回来的，但老人也是无心之举，表弟家里经济条件有限。“都是农村出来的，孩子奶奶觉得没有开封可能没有问题，就带给孩子喝。他们家庭这两年不太走运，孩子父母欠了很多钱，奶奶捡拾废品卖钱也是贴补家用。”\n黎女士说：“事发后，孩子奶奶很后悔，心理压力非常大，说孩子救不回来她赔命，孩子爸爸妈妈还要经常安慰老人，怕老人想不开。”\n据黎女士介绍，辖区民警当天去儿童医院，也向孩子奶奶了解情况，但目前还没有调查结果。\n医院收治时就觉得毒性不正常\n送检检出三甲基硅醇怀疑投毒\n黎女士告诉记者，“9月16号当天就赶紧送到深圳儿童医院抢救，就去做了检测，发现里面含有三甲基硅醇。当时在收治时，医院就觉得这个毒物毒性不正常，当天下午医院就报了警。”\n“9月17号毒物检测出了报告，这属于工业毒品中毒，医院说不排除是有人故意投毒。”\n毒物检测报告单显示，在送检的血液、胃液和不明液体中，检测到三甲基硅醇，其中血液中的浓度为270ng/ml。\n\n毒物检测报告单显示，在送检的血液、胃液和不明液体中，检测到三甲基硅醇\n据黎女士介绍，兄妹俩目前住院救治累计已经花费十几万元，“发生这个事情，救治的费用总要负担。孩子父母都在深圳这边打工，家里经济条件不好，现在治疗费可以亲戚朋友凑一凑，但是后期如果实在太多了，估计也没办法了。”\n', '{\"ops\":[{\"insert\":\"一个多月前，深圳宝安，6岁的哥哥和4岁妹妹喝了奶奶从外面垃圾桶捡回来的未开封的饮料，随即出现呕吐、腹痛症状，被送医抢救。\\n\"},{\"attributes\":{\"width\":\"193\"},\"insert\":{\"image\":\"http://localhost:8080/images/cache/20231023/6c95e362dbfe4b11b3b89a45f07bbd83\"}},{\"insert\":\"\\n4岁1个月大的中毒女孩病情严重\\n10月22日，兄妹俩住院治疗38天，表姑妈黎女士告诉华商报大风新闻记者，“兄妹俩当时分着喝了一小部分，就上吐下泻，后来确定孩子喝了这个饮料中毒，里面检出了三甲基硅醇。”\\n奶奶捡回饮料爷爷帮忙给打开\\n兄妹俩只喝了一点就上吐下泻\\n10月22日，表姑妈黎女士接受华商报大风新闻记者采访表示，6岁的表侄子和4岁的表侄女在深圳儿童医院住院抢救已经38天。\\n黎女士介绍，事发9月16日，孩子60来岁的奶奶拾废品，捡回来一瓶未过期的饮料。\\n“因为捡回来的饮料瓶子根本没有开封，是一个完好无损的饮料瓶，孩子自己都打不开，是孩子爷爷帮忙打开给他们喝的。倒出来只喝了一点，两个孩子都翻了，上吐下泻，兄妹俩应该喝的量是差不多，因为孩子一喝就感觉味道不对，兄妹俩分着喝了一会儿就不行了。”\\n黎女士解释称，虽然是孩子奶奶把饮料捡回来的，但老人也是无心之举，表弟家里经济条件有限。“都是农村出来的，孩子奶奶觉得没有开封可能没有问题，就带给孩子喝。他们家庭这两年不太走运，孩子父母欠了很多钱，奶奶捡拾废品卖钱也是贴补家用。”\\n黎女士说：“事发后，孩子奶奶很后悔，心理压力非常大，说孩子救不回来她赔命，孩子爸爸妈妈还要经常安慰老人，怕老人想不开。”\\n据黎女士介绍，辖区民警当天去儿童医院，也向孩子奶奶了解情况，但目前还没有调查结果。\\n医院收治时就觉得毒性不正常\\n送检检出三甲基硅醇怀疑投毒\\n黎女士告诉记者，“9月16号当天就赶紧送到深圳儿童医院抢救，就去做了检测，发现里面含有三甲基硅醇。当时在收治时，医院就觉得这个毒物毒性不正常，当天下午医院就报了警。”\\n“9月17号毒物检测出了报告，这属于工业毒品中毒，医院说不排除是有人故意投毒。”\\n毒物检测报告单显示，在送检的血液、胃液和不明液体中，检测到三甲基硅醇，其中血液中的浓度为270ng/ml。\\n\"},{\"attributes\":{\"width\":\"275\"},\"insert\":{\"image\":\"http://localhost:8080/images/cache/20231023/85903aad7db046a38d78ac5eb4714504\"}},{\"insert\":\"\\n毒物检测报告单显示，在送检的血液、胃液和不明液体中，检测到三甲基硅醇\\n据黎女士介绍，兄妹俩目前住院救治累计已经花费十几万元，“发生这个事情，救治的费用总要负担。孩子父母都在深圳这边打工，家里经济条件不好，现在治疗费可以亲戚朋友凑一凑，但是后期如果实在太多了，估计也没办法了。”\\n\"}]}', 1, 2, '2023-10-23 19:15:08', 0, 0, 0);
INSERT INTO `db_topic` (`id`, `title`, `intro`, `content`, `uid`, `type`, `time`, `top`, `locked`, `invisible`) VALUES (18, '新人报道，请多指教', NULL, '{\"ops\":[{\"insert\":\"大家好，我今天第一次注册，请问需要注意什么？\\n\"}]}', 2, 3, '2023-10-28 18:23:03', 0, 1, 1);
COMMIT;

-- ----------------------------
-- Table structure for db_topic_comment
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_comment`;
CREATE TABLE `db_topic_comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `tid` int DEFAULT NULL,
  `content` text,
  `time` datetime DEFAULT NULL,
  `quote` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_topic_comment
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (1, 1, 15, '{\"ops\":[{\"insert\":\"我是一个评论大叔大婶\\n\"}]}', '2023-10-28 16:36:09', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (2, 1, 15, '{\"ops\":[{\"insert\":\"大好时机大会上打卡斯柯达\\n\"}]}', '2023-10-28 17:14:51', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (3, 1, 15, '{\"ops\":[{\"insert\":\"阿萨德回到家啊好看大家啊受打击卡手打\\n\"}]}', '2023-10-28 17:18:09', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (4, 1, 15, '{\"ops\":[{\"insert\":\"萨达大好时机大开杀戒打撒科技大\\n\"}]}', '2023-10-28 17:23:18', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (5, 1, 15, '{\"ops\":[{\"insert\":\"阿莎大家啊回到家阿是打卡机阿达\\n\"}]}', '2023-10-28 17:23:22', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (6, 1, 15, '{\"ops\":[{\"insert\":\"阿莎大家啊回到家阿是打卡机阿达\\n\"}]}', '2023-10-28 17:23:22', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (7, 1, 15, '{\"ops\":[{\"insert\":\"阿莎大家啊回到家阿是打卡机阿达\\n\"}]}', '2023-10-28 17:23:22', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (8, 1, 15, '{\"ops\":[{\"insert\":\"我是一个评论大叔大婶\\n\"}]}', '2023-10-28 16:36:09', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (9, 1, 15, '{\"ops\":[{\"insert\":\"大好时机大会上打卡斯柯达\\n\"}]}', '2023-10-28 17:14:51', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (15, 1, 15, '{\"ops\":[{\"insert\":\"我是最新评论的评论\\n\"}]}', '2023-10-28 17:56:26', 13);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (17, 1, 15, '{\"ops\":[{\"insert\":\"我是评论的评论的评论的评论\\n\"}]}', '2023-10-28 17:59:54', 16);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (18, 1, 10, '{\"ops\":[{\"insert\":\"我是第一条评论\\n\"}]}', '2023-10-28 18:11:16', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (19, 1, 10, '{\"ops\":[{\"insert\":\"我是楼肿瘤\\n\"}]}', '2023-10-28 18:11:22', 18);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (20, 1, 18, '{\"ops\":[{\"insert\":\"我草牛逼\\n\"}]}', '2023-10-28 18:25:46', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (22, 2, 18, '{\"ops\":[{\"insert\":\"萨达撒大声地\\n\"}]}', '2023-10-28 18:26:07', 20);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (23, 2, 16, '{\"ops\":[{\"insert\":\"牛逼\\n\"}]}', '2023-10-28 18:58:06', -1);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (24, 2, 18, '{\"ops\":[{\"insert\":\"等不及阿萨德黑科技阿斯达户籍卡撒\\n\"}]}', '2023-10-28 19:04:15', 20);
INSERT INTO `db_topic_comment` (`id`, `uid`, `tid`, `content`, `time`, `quote`) VALUES (25, 2, 18, '{\"ops\":[{\"insert\":\"苹萨达撒大声地\\n\"}]}', '2025-12-25 22:50:24', 20);
COMMIT;

-- ----------------------------
-- Table structure for db_topic_interact_collect
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_collect`;
CREATE TABLE `db_topic_interact_collect` (
  `tid` int DEFAULT NULL,
  `uid` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  UNIQUE KEY `tid_uid_collect` (`tid`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_topic_interact_collect
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (16, 1, '2023-10-23 21:19:09');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (15, 1, '2023-10-23 21:50:35');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (13, 1, '2023-10-28 18:23:28');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (13, 2, '2023-10-28 18:23:28');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (14, 4, '2023-10-28 18:23:28');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (18, 2, '2023-10-28 18:23:28');
INSERT INTO `db_topic_interact_collect` (`tid`, `uid`, `time`) VALUES (16, 2, '2023-10-28 18:23:32');
COMMIT;

-- ----------------------------
-- Table structure for db_topic_interact_like
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_interact_like`;
CREATE TABLE `db_topic_interact_like` (
  `tid` int DEFAULT NULL,
  `uid` int DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  UNIQUE KEY `tid_uid_like` (`tid`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_topic_interact_like
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (16, 1, '2023-10-23 21:19:10');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (13, 1, '2023-10-28 18:23:27');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (13, 2, '2023-10-28 18:23:27');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (12, 2, '2023-10-28 18:23:27');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (14, 4, '2023-10-28 18:23:27');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (18, 2, '2023-10-28 18:23:27');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (16, 2, '2023-10-28 18:23:32');
INSERT INTO `db_topic_interact_like` (`tid`, `uid`, `time`) VALUES (18, 1, '2023-10-28 18:56:36');
COMMIT;

-- ----------------------------
-- Table structure for db_topic_type
-- ----------------------------
DROP TABLE IF EXISTS `db_topic_type`;
CREATE TABLE `db_topic_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `desc` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1249853443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of db_topic_type
-- ----------------------------
BEGIN;
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (1, '日常闲聊', '在这里分享你的各种日常好贱啊带回家阿萨德', '#1E56FF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (2, '真诚交友', '在校园里寻找与自己志同道合的朋友', '#CE1EFF');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (3, '问题反馈', '反馈你在校园里遇到的问题', '#E07373');
INSERT INTO `db_topic_type` (`id`, `name`, `desc`, `color`) VALUES (4, '恋爱官宣', '向大家展示你的恋爱成果', '#E0CE73');
COMMIT;

-- ----------------------------
-- Table structure for ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '所属用户',
    `title` VARCHAR(100) DEFAULT '新对话' COMMENT '对话标题',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话';

-- ----------------------------
-- Table structure for ai_conversation_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation_message`;
CREATE TABLE IF NOT EXISTS `ai_conversation_message` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id` INT NOT NULL COMMENT '所属对话ID',
    `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容(JSON格式)',
    `message_type` VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/tool_call/tool_result',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息';

SET FOREIGN_KEY_CHECKS = 1;
