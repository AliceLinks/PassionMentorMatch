const api = require('../../utils/request.js');

Page({
  data: {
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    avatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    nickname: '游客',
    real_name: '',
    phone: ''
  },
  onLoad() {
    const u = wx.getStorageSync('userInfo');
    if (u) {
      this.setData({
        avatarUrl: u.avatar || this.data.defaultAvatarUrl,
        nickname: u.nickname || '游客',
        real_name: u.real_name || '',
        phone: u.phone || ''
      });
    }
  },
  onChooseAvatar(e) {
    const avatarUrl = e.detail && e.detail.avatarUrl;
    if (avatarUrl) this.setData({ avatarUrl });
  },
  onNicknameInput(e) { this.setData({ nickname: e.detail.value }); },
  onRealNameInput(e) { this.setData({ real_name: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onSave() {
    const payload = {
      avatar: this.data.avatarUrl,
      nickname: this.data.nickname || '游客',
      real_name: this.data.real_name || null,
      phone: this.data.phone || null
    };
    api.put('/user/profile', payload)
      .then(user => {
        wx.setStorageSync('userInfo', user);
        const app = getApp();
        if (app && app.globalData) app.globalData.user = user;
        wx.showToast({ title: '已保存', icon: 'success' });
        wx.navigateBack();
      })
      .catch(err => {
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
      });
  }
})