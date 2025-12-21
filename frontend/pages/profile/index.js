const api = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    user: {
      avatar: "https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0",
      nickname: "游客",
      points: 0,
      level: 1
    },
    hasToken: false,
    userInfo: null,
    cards: []
  },

  onLoad() {
    const u = wx.getStorageSync('userInfo');
    if (u) {
      this.setData({
        user: { ...this.data.user, avatar: u.avatar, nickname: u.nickname }
      });
    }
  },

  onShow() {
    const u = wx.getStorageSync('userInfo');
    if (u) {
      this.setData({
        user: { ...this.data.user, avatar: u.avatar, nickname: u.nickname }
      });
    }
    // 选中第三个 tab（索引 2）
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }

    const token = wx.getStorageSync('token');
    const hasToken = !!token;
    this.setData({ hasToken });

    if (hasToken) {
      this.loadData();
    } else {
      this.setData({ userInfo: null, cards: [] });
    }
  },

  loadData() {
    const token = wx.getStorageSync('token');
    if (!token) return;

    // 获取用户信息
    api.get('/user/profile').then(res => {
      this.setData({
        userInfo: res,
        user: {
          ...this.data.user,
          avatar: res.avatar || this.data.user.avatar,
          nickname: res.nickname || this.data.user.nickname
        }
      });
    });

    // 获取导师卡
    api.get('/user/cards').then(res => {
      this.setData({ cards: res || [] });
    });
  },

  goReservations() {
    wx.navigateTo({ url: '/pages/my-reservations/index' });
  },

  // 点击“手机号登录 / 注册”
  handleLogin() {
    wx.navigateTo({ url: '/pages/login/index' });
  },

  // 编辑个人资料
  goEdit() {
    wx.navigateTo({ url: '/pages/profile/edit' });
  },

  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/login/index' });
  },

  // 退出登录（可选加一个）
  logout() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    this.setData({
      hasToken: false,
      userInfo: null,
      cards: [],
      user: {
        ...this.data.user,
        nickname: '游客'
      }
    });
  }
});
