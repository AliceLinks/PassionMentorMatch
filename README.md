# Passion街舞社课程预约微信小程序 - 使用说明

## 项目简介
本项目是南京大学Passion街舞社课程预约微信小程序，包含用户端和管理员端两个部分，实现了课程预约、课程管理、信息展示等功能。

## 系统基本配置要求

### 前端运行环境
- 微信开发者工具 Stable 版（建议 v1.06.2403080 及以上版本）
- Node.js v14.0.0 及以上版本
- 微信小程序基础库版本 2.24.0 及以上

### 后端运行环境
- JDK 1.8 及以上版本
- Maven 3.6.0 及以上版本（项目已包含 Maven  wrapper，可直接使用）
- MySQL 8.0 及以上版本
- 操作系统：Windows/macOS/Linux 均可

## 运行步骤

### 1. 后端启动
1. 进入后端目录：
   ```bash
   cd PassionMentorMatch-user-admin-apart-2/backend
   ```

2. 配置数据库连接（需自行在配置文件中设置，`application.yml`）：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mentor?useSSL=false&serverTimezone=UTC
   spring.datasource.username=数据库用户名
   spring.datasource.password=数据库密码
   ```

3. 使用 Maven 构建并启动：
   - Windows 系统：
     ```bash
     mvnw spring-boot:run
     ```
   - macOS/Linux 系统：
     ```bash
     ./mvnw spring-boot:run
     ```

### 2. 前端（用户端）启动
1. 打开微信开发者工具
2. 点击「导入项目」，选择 `frontend` 目录
3. 填写小程序 AppID（若无正式 AppID，可选择「测试号」）
4. 点击「确定」即可加载项目，加载完成后自动运行

### 3. 管理员端启动
1. 打开微信开发者工具（可新建窗口）
2. 点击「导入项目」，选择 `frontend-admin` 目录
3. 填写小程序 AppID（与用户端可使用同一测试号）
4. 点击「确定」即可加载项目，加载完成后自动运行

## 主要功能说明

### 用户端
- 主页：本周推荐课程、课程介绍、路线图
- 约课：每日课程及相关信息、课程预约\取消
- 我的：可视化导师卡、预约记录

### 管理员端
- 课程管理：取消/修改课程信息
- 批量发布课程
- 导师卡发放（根据手机号搜索，绑定照片）
- 签到核销（根据照片）
- 内容管理（课程介绍、路线图）
