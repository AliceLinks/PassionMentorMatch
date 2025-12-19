const api = require('../../utils/request.js');
const app = getApp();

Page({
    data: {
        user: { avatar: "https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0", nickname: "游客", points: 0, level: 1 }
    },

    onLoad() {
        const u = wx.getStorageSync('userInfo')
        if (u) {
          this.setData({ user: { ...this.data.user, avatar: u.avatar, nickname: u.nickname } })
        }
      },

    onShow() {
        const u = wx.getStorageSync('userInfo')
        if (u) this.setData({ user: { ...this.data.user, avatar: u.avatar, nickname: u.nickname } })
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().setData({ selected: 4 })
        }
    },

    loadData() {
        const token = wx.getStorageSync('token');
        if (!token) return; // 未登录不请求
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

    goEdit() {
        wx.navigateTo({ url: '/pages/profile' })
    },
    goAdmin() {
        wx.showToast({ title: '仅管理员使用', icon: 'none' });
    }
});
