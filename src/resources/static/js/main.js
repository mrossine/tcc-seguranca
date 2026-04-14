// Main JavaScript for Segurança Fatec ZL

// Initialize tooltips
document.addEventListener('DOMContentLoaded', function() {
	// Initialize Bootstrap tooltips
	var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
	tooltipTriggerList.map(function(tooltipTriggerEl) {
		return new bootstrap.Tooltip(tooltipTriggerEl);
	});

	// Add fade-in animation to cards
	const cards = document.querySelectorAll('.card');
	cards.forEach((card, index) => {
		card.style.animationDelay = `${index * 0.1}s`;
		card.classList.add('fade-in');
	});
});

// Format date to Brazilian format
function formatDate(dateString) {
	const date = new Date(dateString);
	return date.toLocaleString('pt-BR');
}

// Show toast notification
function showToast(message, type = 'success') {
	const toastContainer = document.getElementById('toast-container');
	if (!toastContainer) {
		const container = document.createElement('div');
		container.id = 'toast-container';
		container.style.position = 'fixed';
		container.style.bottom = '20px';
		container.style.right = '20px';
		container.style.zIndex = '9999';
		document.body.appendChild(container);
	}

	const toast = document.createElement('div');
	toast.className = `toast align-items-center text-white bg-${type} border-0`;
	toast.role = 'alert';
	toast.ariaLive = 'assertive';
	toast.ariaAtomic = 'true';

	toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

	document.getElementById('toast-container').appendChild(toast);
	const bsToast = new bootstrap.Toast(toast);
	bsToast.show();

	toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

// Confirm action with dialog
function confirmAction(message) {
	return confirm(message);
}

// Load user statistics for dashboard
function loadUserStats() {
	fetch('/api/estatisticas/ocorrencias')
		.then(response => response.json())
		.then(data => {
			if (document.getElementById('totalAlertas')) {
				document.getElementById('totalAlertas').textContent = data.totalAlertasAtivos || 0;
				document.getElementById('totalCaronas').textContent = data.totalCaronasDisponiveis || 0;
				document.getElementById('totalUsuarios').textContent = data.totalUsuarios || 0;
			}
		})
		.catch(error => console.error('Error loading stats:', error));
}

// Geo location helper
function getCurrentLocation() {
	return new Promise((resolve, reject) => {
		if (!navigator.geolocation) {
			reject('Geolocation not supported');
			return;
		}

		navigator.geolocation.getCurrentPosition(
			position => resolve({
				latitude: position.coords.latitude,
				longitude: position.coords.longitude
			}),
			error => reject(error.message)
		);
	});
}

// Auto-fill location in forms
async function autoFillLocation() {
	try {
		const location = await getCurrentLocation();
		const locationInput = document.getElementById('localizacao');
		if (locationInput) {
			// Here you would typically use a reverse geocoding service
			// For now, just set coordinates
			document.getElementById('latitude').value = location.latitude;
			document.getElementById('longitude').value = location.longitude;
			showToast('Localização detectada com sucesso!', 'success');
		}
	} catch (error) {
		showToast('Não foi possível detectar sua localização: ' + error, 'danger');
	}
}