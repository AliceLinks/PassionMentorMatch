// pages/register/index.js
const api = require('../../utils/request.js');

Page({
  data: {
    phone: '',
    password: '',
    realName: '',
    avatarUrl: '',
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    loading: false
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },
  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },
  onNameInput(e) {
    this.setData({ realName: e.detail.value });
  },
  onChooseAvatar(e) {
    const avatarUrl = e.detail && e.detail.avatarUrl;
    if (avatarUrl) this.setData({ avatarUrl });
  },
  onRegister() {
    if (this.data.loading) return;
    const { phone, password, realName, avatarUrl, defaultAvatarUrl } = this.data;
    if (!phone || !password || !realName) {
      wx.showToast({ title: '请填写姓名、手机号和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    api.post('/user/register', {
      phone,
      password,
      realName,
      avatar: avatarUrl || defaultAvatarUrl
    })
      .then(() => {
        wx.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => {
          wx.navigateBack({ delta: 1 });
        }, 1000);
      })
      .catch(err => {
        wx.showToast({ title: err?.message || '注册失败', icon: 'none' });
      })
      .finally(() => this.setData({ loading: false }));
  }
});