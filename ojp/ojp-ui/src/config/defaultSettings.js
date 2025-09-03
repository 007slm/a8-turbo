/**
 * Ant Design Pro 默认配置 - OJP 管理系统
 */
export default {
  // 导航的主题，'light' | 'dark' | 'realDark'
  navTheme: 'light',
  // 主色调
  colorPrimary: 'var(--primary-color)',
  // 布局类型 'side' | 'top' | 'mix'
  layout: 'side',
  // 内容区域宽度 'Fluid' | 'Fixed'
  contentWidth: 'Fluid',
  // 固定头部
  fixedHeader: true,
  // 固定侧边栏
  fixSiderbar: true,
  // 色弱模式
  colorWeak: false,
  // 标题
  title: 'OJP 管理系统',
  // 主题色
  primaryColor: 'var(--primary-color)',
  // 分割菜单
  splitMenus: false,
  // 头部高度
  headerHeight: 56,
  // 禁用移动端
  isMobile: false,
  // 菜单的折叠收起事件
  onCollapse: () => {},
  // 菜单的打开事件
  onMenuHeaderClick: () => {},
  // 自定义菜单的宽度
  siderWidth: 220,
  // 是否显示设置抽屉
  settings: false,
  // logo 配置
  logo: false,
  // 菜单配置
  menu: {
    locale: false,
    autoClose: false,
  },
  // 页脚配置
  footerRender: false,
  // 面包屑配置
  breadcrumbRender: (routers = []) => {
    return routers;
  },
  // 页面标题配置
  pageTitleRender: (props, defaultPageTitle) => {
    return defaultPageTitle;
  },
};