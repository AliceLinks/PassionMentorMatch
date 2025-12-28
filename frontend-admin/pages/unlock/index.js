Page({
  data: {
    inputValue: '',
    error: ''
  },
  onInput(e) {
    this.setData({ inputValue: e.detail.value, error: '' });
  },
  onSubmit() {
    const val = (this.data.inputValue || '').trim();
    if (!val) { this.setData({ error: '请输入密码' }); return; }
    const request = require('../../utils/request.js');
    request.post('/api/admin/login', { username: 'admin', password: val })
      .then((data) => {
        const token = data && data.token;
        if (token) {
          wx.setStorageSync('token', token);
          wx.setStorageSync('unlocked', true);
          wx.redirectTo({ url: '/pages/admin/home/index' });
        } else {
          this.setData({ error: '登录失败' });
        }
      })
      .catch((err) => {
        console.error('unlock login err', err);
        const msg = (err && err.message) || (err && err.msg) || '登录失败';
        this.setData({ error: msg });
      });
  }
});
