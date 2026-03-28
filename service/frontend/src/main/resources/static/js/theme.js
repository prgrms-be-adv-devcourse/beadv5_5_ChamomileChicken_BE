// 아이콘 상태 업데이트
function updateThemeIcon() {
    const isDark = document.documentElement.classList.contains('dark');
    document.querySelectorAll('.theme-icon-dark').forEach(el => el.classList.toggle('hidden', !isDark));
    document.querySelectorAll('.theme-icon-light').forEach(el => el.classList.toggle('hidden', isDark));
}

// 토글
function toggleTheme() {
    const isDark = document.documentElement.classList.contains('dark');
    if (isDark) {
        document.documentElement.classList.remove('dark');
        localStorage.setItem('theme', 'light');
    } else {
        document.documentElement.classList.add('dark');
        localStorage.setItem('theme', 'dark');
    }
    updateThemeIcon();
}

document.addEventListener('DOMContentLoaded', updateThemeIcon);