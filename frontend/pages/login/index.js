// pages/login/index.js
const app = getApp();
const api = require('../../utils/request.js');

Page({
  data: {
    loading: false,
    phone: '',
    password: '',
    // false = 登录；true = 注册
    isRegisterMode: false
  },

  onShow() {
    // 已有 token 则直接拿用户信息并返回
    const token = wx.getStorageSync('token');
    if (token) {
      this.afterLogin();
    }
  },

  // 绑定手机号输入
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value.trim() });
  },

  // 绑定密码输入
  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  // 切换 登录 / 注册 模式
  toggleMode() {
    this.setData({ isRegisterMode: !this.data.isRegisterMode });
  },

  // 点击“登录 / 注册”按钮
  onSubmit() {
    if (this.data.loading) return;
    const { phone, password, isRegisterMode } = this.data;
    if (!phone || !password) {
      wx.showToast({ title: '请输入手机号和密码', icon: 'none' });
      return;
    }
    if (isRegisterMode) {
      this.handleRegister();
    } else {
      this.handleLogin();
    }
  },

  // 注册：POST /api/user/register
  handleRegister() {
    this.setData({ loading: true });
    api.post('/user/register', {
      phone: this.data.phone,
      password: this.data.password
    })
      .then(res => {
        // 按接口文档：data = { token, user }
        const token = res.token;
        const user = res.user;
        if (app && app.setLoginState) {
          app.setLoginState(token, user);
        } else {
          wx.setStorageSync('token', token);
          wx.setStorageSync('userInfo', user);
        }
        wx.showToast({ title: '注册成功', icon: 'success' });
        wx.navigateBack({ delta: 1 });
      })
      .catch(err => {
        wx.showToast({ title: err.message || '注册失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  // 登录：POST /api/user/login
  handleLogin() {
    this.setData({ loading: true });
    api.post('/user/login', {
      phone: this.data.phone,
      password: this.data.password
    })
      .then(res => {
        const token = res.token;
        const user = res.user;
        if (app && app.setLoginState) {
          app.setLoginState(token, user);
        } else {
          wx.setStorageSync('token', token);
          wx.setStorageSync('userInfo', user);
        }
        wx.showToast({ title: '登录成功', icon: 'success' });
        wx.navigateBack({ delta: 1 });
      })
      .catch(err => {
        wx.showToast({ title: err.message || '登录失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  // 兼容：如果已经有 token，则拉一次 profile 再返回
  afterLogin() {
    api.get('/user/profile')
      .then(user => {
        if (app && app.setLoginState) {
          app.setLoginState(wx.getStorageSync('token'), user);
        } else {
          wx.setStorageSync('userInfo', user);
        }
        wx.showToast({ title: '登录成功', icon: 'success' });
        wx.navigateBack({ delta: 1 });
      })
      .catch(() => {
        wx.navigateBack({ delta: 1 });
      });
  }
});