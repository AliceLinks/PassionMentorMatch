const app = getApp();
const api = require('../../utils/request.js');

Page({
  data: {
    loading: false,
    phone: '',
    password: '',
    // 使用同一 Logo 源，可替换为上传后的 URL
    logoUrl: wx.getStorageSync('APP_LOGO') || '/images/logo.png'
  },
  onShow() {
    // 已有 token 则跳过
    const token = wx.getStorageSync('token');
    if (token) {
      this.afterLogin();
    }
  },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onPasswordInput(e) { this.setData({ password: e.detail.value }); },

  onLogin() {
    if (this.data.loading) return;
    const { phone, password } = this.data;
    if (!phone || !password) {
      wx.showToast({ title: '请输入手机号和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    api.post('/user/login', { phone, password })
      .then(res => {
        if (res && res.token) {
          wx.setStorageSync('token', res.token);
          wx.setStorageSync('userInfo', res.user || {});
          wx.showToast({ title: '登录成功', icon: 'success' });
          wx.switchTab({ url: '/pages/home/index' });
        } else {
          wx.showToast({ title: res && res.message ? res.message : '登录失败', icon: 'none' });
        }
      })
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
        wx.switchTab({ url: '/pages/home/index' });
      })
      .catch(() => {
        // 容错：仍然返回主页
        wx.switchTab({ url: '/pages/home/index' });
      });
  }
  ,
  onRegister() {
    wx.navigateTo({ url: '/pages/register/index' });
  }
});