const api = require('../../utils/request.js');

Page({
  data: {
    // 移除 defaultAvatarUrl、avatarUrl
    nickname: '游客',
    real_name: '',
    phone: '',
    oldPassword: '',   
    newPassword: ''    
  },
  onLoad() {
    const u = wx.getStorageSync('userInfo');
    if (u) {
      this.setData({
        // 移除 avatarUrl 相关
        nickname: u.nickname || '游客',
        real_name: u.realName || '',
        phone: u.phone || ''
      });
    }
  },
  // 移除 onChooseAvatar 方法

  onNicknameInput(e) { this.setData({ nickname: e.detail.value }); },
  onRealNameInput(e) { this.setData({ real_name: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },

  onOldPwdInput(e) { this.setData({ oldPassword: e.detail.value }); },
  onNewPwdInput(e) { this.setData({ newPassword: e.detail.value }); },

  onSave() {
    if (this.data.oldPassword || this.data.newPassword) {
      if (!this.data.oldPassword || !this.data.newPassword) {
        wx.showToast({ title: '请同时填写原密码和新密码', icon: 'none' });
        return;
      }
      if (this.data.newPassword.length < 6) {
        wx.showToast({ title: '新密码至少6位', icon: 'none' });
        return;
      }
    }

    const payload = {
      // 移除 avatar 字段
      nickname: this.data.nickname || '游客',
      realName: this.data.real_name || null,
      phone: this.data.phone || null
    };

    if (this.data.oldPassword && this.data.newPassword) {
      payload.oldPassword = this.data.oldPassword;
      payload.newPassword = this.data.newPassword;
    }

    api.put('/user/profile', payload)
      .then(user => {
        wx.setStorageSync('userInfo', user);
        const app = getApp();
        if (app && app.globalData) app.globalData.user = user;
        this.setData({ oldPassword: '', newPassword: '' });
        wx.showToast({ title: '已保存', icon: 'success' });
        wx.navigateBack();
      })
      .catch(err => {
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
      });
  }
})