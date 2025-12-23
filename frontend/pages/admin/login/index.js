Page({
  data: {
    phone: '',
    password: '',
    loading: false
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  onSubmit() {
    const { phone, password } = this.data;
    if (!phone || !password) {
      wx.showToast({ title: '请输入手机号和密码', icon: 'none' });
      return;
    }

    const base = wx.getStorageSync('BASE_URL') || 'http://localhost:8080/api';
    const apiPrefix = base.endsWith('/api') ? base : `${base}/api`;

    this.setData({ loading: true });
    wx.request({
      url: `${apiPrefix}/admin/login`,
      method: 'POST',
      data: { phone, password },
      success: (res) => {
        const data = res.data;
        if (res.statusCode === 200 && (data.code === 0 || data.code === 200)) {
          const token = data.data.token;
          wx.setStorageSync('ADMIN_TOKEN', token);
          wx.showToast({ title: '登录成功', icon: 'success' });
          wx.redirectTo({ url: '/pages/admin/home/index' });
        } else {
          wx.showToast({ title: data.message || '登录失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  }
});