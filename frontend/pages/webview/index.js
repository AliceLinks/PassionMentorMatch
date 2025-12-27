Page({
  onLoad(query) {
    const url = decodeURIComponent(query.url || '');
    this.setData({ url });
    console.log('webview url:', url);
  },
  data: {
    url: ''
  }
});
