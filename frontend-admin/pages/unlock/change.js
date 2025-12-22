Page({
  data: {
    oldPwd: '',
    newPwd: '',
    confirmPwd: '',
    error: '',
    success: ''
  },
  onOldInput(e) { this.setData({ oldPwd: e.detail.value, error: '', success: '' }); },
  onNewInput(e) { this.setData({ newPwd: e.detail.value, error: '', success: '' }); },
  onConfirmInput(e) { this.setData({ confirmPwd: e.detail.value, error: '', success: '' }); },
  onSubmit() {
    const { oldPwd, newPwd, confirmPwd } = this.data;
    const saved = wx.getStorageSync('unlock_pwd') || '123456';
    if (!oldPwd || !newPwd || !confirmPwd) {
      this.setData({ error: '请填写完整', success: '' }); return;
    }
    if (oldPwd !== saved) {
      this.setData({ error: '原密码错误', success: '' }); return;
    }
    if (newPwd.length !== 6 || /\D/.test(newPwd)) {
      this.setData({ error: '新密码必须为6位数字', success: '' }); return;
    }
    if (newPwd !== confirmPwd) {
      this.setData({ error: '两次输入的新密码不一致', success: '' }); return;
    }
    wx.setStorageSync('unlock_pwd', newPwd);
    this.setData({ error: '', success: '密码修改成功！' });
    setTimeout(() => { wx.navigateBack(); }, 1200);
  }
});
