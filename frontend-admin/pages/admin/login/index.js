Page({
  data: { username: '', password: '' },
  onUsername(e){ this.setData({ username: e.detail.value }); },
  onPassword(e){ this.setData({ password: e.detail.value }); },
  onLogin(){
    const { username, password } = this.data;
    if(!username || !password){ wx.showToast({ title: '请输入账号密码', icon: 'none' }); return; }
    const base = wx.getStorageSync('BASE_URL');
    if(!base){ wx.showToast({ title: '未配置BASE_URL', icon: 'none' }); return; }
    wx.request({
      url: `${base}/admin/login`,
      method: 'POST',
      data: { username, password },
      header: { 'content-type': 'application/json' },
      success: (res) => {
        if(res.statusCode===200 && res.data && res.data.code===200){
          const token = res.data.data && res.data.data.token;
          if(token){ wx.setStorageSync('ADMIN_TOKEN', token); }
          wx.showToast({ title: '登录成功' });
          wx.navigateTo({ url: '/pages/admin/home/index' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '登录失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  }
});