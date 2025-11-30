const BASE_URL = 'http://127.0.0.1:8080/api'; // 根据实际后端地址修改

const request = (url, method = 'GET', data = {}) => {
  const token = wx.getStorageSync('token');
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 401) {
          // Token 失效，跳转登录
          wx.navigateTo({ url: '/pages/login/index' });
          reject(res);
        } else if (res.data.code === 200) {
          resolve(res.data.data);
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
};

module.exports = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  del: (url, data) => request(url, 'DELETE', data)
};