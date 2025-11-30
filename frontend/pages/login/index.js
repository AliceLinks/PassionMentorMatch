// pages/login/index.js
const app = getApp();
const api = require('../../utils/request.js');

Page({
  data: {
    loading: false
  },
  onShow() {
    // 已有 token 则跳过
    const token = wx.getStorageSync('token');
    if (token) {
      this.afterLogin();
    }
  },
  onLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    app.doLogin()
      .then(() => this.afterLogin())
      .catch(() => wx.showToast({ title: '登录失败', icon: 'none' }))
      .finally(() => this.setData({ loading: false }));
  },
  afterLogin() {
    // 拉取用户信息并返回
    api.get('/user/profile')
      .then(user => {
        wx.setStorageSync('userInfo', user);
        if (app && app.globalData) app.globalData.user = user;
        wx.showToast({ title: '登录成功', icon: 'success' });
        wx.navigateBack({ delta: 1 });
      })
      .catch(() => {
        // 容错：仍然返回
        wx.navigateBack({ delta: 1 });
      });
  }
})