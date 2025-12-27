const api = require('./utils/request.js');

App({
  onLaunch() {
    // 初始化云开发环境
    if (wx.cloud) {
      wx.cloud.init({
        env: 'cloud1-8gzef1rzcda5e9d2'
      });
    }
    // 可在此初始化全局数据或做健康检查
    this.globalData.language = 'zh';
    this.globalData.adminInfo = wx.getStorageSync('adminInfo') || {
      nickname: '管理员',
      avatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0'
    };
    // 启动时如已解锁则跳转首页
    if (wx.getStorageSync('unlocked')) {
      wx.redirectTo({ url: '/pages/admin/home/index' });
    }
  },
  globalData: { user: null, language: 'zh', mapKey: 'G42BZ-6LWLT-JVXXV-LQWMS-JCD2H-JCFJM', loginCode: '' }
});