Page({
  data: { username: '', password: '' },
  onUsername(e) { this.setData({ username: e.detail.value }); },
  onPassword(e) { this.setData({ password: e.detail.value }); },
  onLogin() {
    const { username, password } = this.data;
    if (!username || !password) { wx.showToast({ title: '请输入账号密码', icon: 'none' }); return; }
    const request = require('../../../utils/request.js');
    request.post('/api/admin/login', { username, password })
      .then((data) => {
        const token = data && data.token;
        if (token) {
          wx.setStorageSync('token', token); // 统一 key，供 request.js 使用
          wx.setStorageSync('unlocked', true);
        }
        wx.showToast({ title: '登录成功' });
        wx.navigateTo({ url: '/pages/admin/home/index' });
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '登录失败', icon: 'none' });
      });
  }
});