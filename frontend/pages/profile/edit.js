const api = require('../../utils/request.js');

Page({
  data: {
    // 资料编辑相关
    defaultAvatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    avatarUrl: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
    realName: '',
    phone: '',
    // 密码修改相关
    oldPwd: '',
    newPwd: '',
    confirmPwd: '',
    pwdError: '',
    pwdSuccess: ''
  },
  onLoad() {
    const u = wx.getStorageSync('userInfo');
    if (u) {
      this.setData({
        avatarUrl: u.avatar || this.data.defaultAvatarUrl,
        realName: u.realName || '',
        phone: u.phone || ''
      });
    }
  },
  // 资料编辑相关
  onRealNameInput(e) { this.setData({ realName: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onSave() {
    // 先校验密码逻辑
    const { oldPwd, newPwd, confirmPwd } = this.data;
    let pwdError = '';
    if (oldPwd || newPwd || confirmPwd) {
      const saved = wx.getStorageSync('profile_pwd') || '123456';
      if (!oldPwd || !newPwd || !confirmPwd) {
        wx.showToast({ title: '请填写完整密码信息', icon: 'none' }); return;
      }
      if (oldPwd !== saved) {
        wx.showToast({ title: '原密码错误', icon: 'none' }); return;
      }
      if (newPwd !== confirmPwd) {
        wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' }); return;
      }
      // 密码只要非空即可，和后端一致
      wx.setStorageSync('profile_pwd', newPwd);
    }
    // 资料保存逻辑
    const payload = {
      avatar: this.data.avatarUrl,
      realName: this.data.realName || null,
      phone: this.data.phone || null
    };
    api.put('/api/user/profile', payload)
      .then(() => {
        // 保存成功后，重新拉取最新用户信息
        return api.get('/api/user/profile');
      })
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
  },
  onAvatarError() {
    const fallback = 'https://ts2.tc.mm.bing.net/th/id/OIP-C.p6hdmBEvZCMwVcWDVnQr0QAAAA?cb=ucfimg2&ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3';
    const current = this.data.avatarUrl;
    if (current !== fallback) {
      this.setData({ avatarUrl: fallback });
    }
  },
  // 密码修改相关
  onOldInput(e) { this.setData({ oldPwd: e.detail.value, pwdError: '', pwdSuccess: '' }); },
  onNewInput(e) { this.setData({ newPwd: e.detail.value, pwdError: '', pwdSuccess: '' }); },
  onConfirmInput(e) { this.setData({ confirmPwd: e.detail.value, pwdError: '', pwdSuccess: '' }); },
  onPwdSubmit() {
    const { oldPwd, newPwd, confirmPwd } = this.data;
    const saved = wx.getStorageSync('profile_pwd') || '123456';
    if (!oldPwd || !newPwd || !confirmPwd) {
      this.setData({ pwdError: '请填写完整', pwdSuccess: '' }); return;
    }
    if (oldPwd !== saved) {
      this.setData({ pwdError: '原密码错误', pwdSuccess: '' }); return;
    }
    if (newPwd.length !== 6 || /\D/.test(newPwd)) {
      this.setData({ pwdError: '新密码必须为6位数字', pwdSuccess: '' }); return;
    }
    if (newPwd !== confirmPwd) {
      this.setData({ pwdError: '两次输入的新密码不一致', pwdSuccess: '' }); return;
    }
    wx.setStorageSync('profile_pwd', newPwd);
    this.setData({ pwdError: '', pwdSuccess: '密码修改成功！' });
    setTimeout(() => { this.setData({ oldPwd: '', newPwd: '', confirmPwd: '', pwdSuccess: '' }); }, 1200);
  }
});