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
    if (!oldPwd || !newPwd || !confirmPwd) {
      this.setData({ error: '请填写完整', success: '' }); return;
    }
    if (newPwd !== confirmPwd) {
      this.setData({ error: '两次输入的新密码不一致', success: '' }); return;
    }
    const request = require('../../utils/request.js');
    request.post('/api/admin/change-password', { old_password: oldPwd, new_password: newPwd })
      .then(() => {
        this.setData({ error: '', success: '密码修改成功！' });
        setTimeout(() => { wx.navigateBack(); }, 1200);
      })
      .catch((err) => {
        console.error('change pwd err', err);
        const msg = (err && err.message) || (err && err.msg) || '修改失败';
        this.setData({ error: msg, success: '' });
      });
  }
});
