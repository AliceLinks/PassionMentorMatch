// pages/register/index.js
const api = require('../../utils/request.js');

Page({
  data: {
    phone: '',
    password: '',
    realName: '',
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
  onRegister() {
    if (this.data.loading) return;
    const { phone, password, realName, defaultAvatarUrl } = this.data;
    if (!phone || !password || !realName) {
      wx.showToast({ title: '请填写姓名、手机号和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    api.post('/user/register', {
      phone,
      password,
      realName,
      avatar: defaultAvatarUrl
    })
      .then(() => {
        wx.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => {
          wx.navigateBack({ delta: 1 });
        }, 1000);
      })
      .catch(err => {
        const msg = (err && err.message) ? err.message : '注册失败';
        wx.showToast({ title: msg, icon: 'none' });
      })
      .finally(() => this.setData({ loading: false }));
  }
});