const api = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    cards: []
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
    this.loadData();
  },

  loadData() {
    // 获取用户信息
    api.get('/user/profile').then(res => {
      this.setData({ userInfo: res });
    });
    // 获取导师卡
    api.get('/user/cards').then(res => {
      this.setData({ cards: res || [] });
    });
  },

  goReservations() {
    wx.navigateTo({ url: '/pages/my-reservations/index' });
  },

  // 简单的登录逻辑触发
  handleLogin() {
    if (!this.data.userInfo) {
      wx.navigateTo({ url: '/pages/login/index' });
    }
  },

  goAdmin() {
    wx.showToast({ title: '仅管理员使用', icon: 'none' });
  }
});
