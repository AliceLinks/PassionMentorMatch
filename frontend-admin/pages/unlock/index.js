Page({
  data: {
    pwd: '',
    error: '',
    pwdInput: ['', '', '', '', '', ''],
    focusIdx: 0
  },
  onInput(e) {
    let val = e.detail.value.replace(/\D/g, '').slice(0, 6);
    let arr = val.split('');
    while (arr.length < 6) arr.push('');
    this.setData({ pwd: val, pwdInput: arr, focusIdx: val.length });
    if (val.length === 6) this.checkPwd(val);
  },
  checkPwd(val) {
    // 读取本地存储的密码，首次无则为123456
    const correct = wx.getStorageSync('unlock_pwd') || '123456';
    if (val === correct) {
      wx.setStorageSync('unlocked', true);
      wx.redirectTo({ url: '/pages/admin/home/index' });
    } else {
      this.setData({ error: '密码错误', pwd: '', pwdInput: ['', '', '', '', '', ''], focusIdx: 0 });
    }
  },
  onFocus() {
    this.setData({ focusIdx: this.data.pwd.length });
  },
});
