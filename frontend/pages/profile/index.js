const api = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    cards: [],
    isAdminVisible: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
    // 移除手动高亮 tabBar
    this.loadData();
    // 控制管理员入口显示：有 ADMIN_TOKEN 或本地开关 SHOW_ADMIN 为真
    const hasAdminToken = !!wx.getStorageSync('ADMIN_TOKEN');
    const showAdminFlag = !!wx.getStorageSync('SHOW_ADMIN');
    this.setData({ isAdminVisible: hasAdminToken || showAdminFlag });
  },

  loadData() {
    // 先确保登录拿到令牌，避免 401
    const app = getApp();
    const ensureLogin = app && typeof app.doLogin === 'function' ? app.doLogin() : Promise.resolve();
    ensureLogin
      .then(() => {
        // 获取用户信息
        return api.get('/api/user/profile');
      })
      .then(res => {
        console.log('userInfo:', res, res.data);
        this.setData({ userInfo: res.data || res });
      })
      .catch(() => {
        // 未登录或失败时，引导登录
        wx.navigateTo({ url: '/pages/login/index' });
      });
    // 获取导师卡（并行，登录后也能访问）
    ensureLogin
      .then(() => api.get('/api/user/cards'))
      .then(res => {
        this.setData({ cards: res || [] });
      })
      .catch(() => {
        this.setData({ cards: [] });
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

  goAdminHome() {
    // 进入管理员总览页面
    wx.navigateTo({ url: '/pages/admin/home/index' });
  },
  goProfileEdit() {
    wx.navigateTo({ url: '/pages/profile/edit' });
  }
  ,
  onAvatarError() {
    const fallback = 'https://ts2.tc.mm.bing.net/th/id/OIP-C.p6hdmBEvZCMwVcWDVnQr0QAAAA?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3';
    const ui = this.data.userInfo || {};
    if (!ui.avatar || ui.avatar === fallback) return;
    ui.avatar = fallback;
    this.setData({ userInfo: ui });
  },

  logout() {
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('token'); // 修正为小写
    wx.removeStorageSync('TOKEN'); // 兼容历史
    wx.removeStorageSync('ADMIN_TOKEN');
    this.setData({ userInfo: null });
    wx.redirectTo({ url: '/pages/login/index' });
  },
});
