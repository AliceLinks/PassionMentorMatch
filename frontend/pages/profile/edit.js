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
        real_name: u.realName || '',
        phone: u.phone || ''
      });
    }
  },
  onChooseAvatar(e) {
    const tempFilePath = e.detail && e.detail.avatarUrl; // 微信头像/本地临时路径
    if (!tempFilePath) return;

    const token = wx.getStorageSync('token');
    wx.showLoading({ title: '上传中...', mask: true });

    wx.uploadFile({
      url: 'http://127.0.0.1:8080/api/image/upload',
      filePath: tempFilePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200) {
            const url = data.data; 
            this.setData({ avatarUrl: url });
            wx.showToast({ title: '上传成功', icon: 'success' });
          } else {
            wx.showToast({ title: data.message || '上传失败', icon: 'none' });
          }
        } catch (err) {
          wx.showToast({ title: '上传失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },
  onNicknameInput(e) { this.setData({ nickname: e.detail.value }); },
  onRealNameInput(e) { this.setData({ real_name: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onSave() {
    const payload = {
      avatar: this.data.avatarUrl,          
      nickname: this.data.nickname || '游客',
      realName: this.data.real_name || null,
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