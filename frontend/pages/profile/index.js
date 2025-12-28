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
    cards: [],
    bannerImages: [
      '/images/1.jpg',
      '/images/2.jpg',
      '/images/3.png'
    ]
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
          // 优先实名头像
          avatar: res.real_name_image || this.data.user.avatar,
          nickname: res.nickname || this.data.user.nickname
        }
      });
    });

    // 获取导师卡
    api.get('/user/cards').then(res => {
      // 若有实名头像，优先展示
      let avatar = this.data.user.avatar;
      if (res && res.length > 0 && res[0].real_name_image) {
        avatar = res[0].real_name_image;
      }
      this.setData({ cards: res || [], 'user.avatar': avatar });
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
