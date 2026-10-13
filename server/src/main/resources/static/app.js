"use strict";

// Centre of Kozhikode, used until the phone gives us a position.
const DEFAULT_CENTER = [11.2588, 75.7804];

const $ = (sel) => document.querySelector(sel);

let map, marker;
let position = null;      // { lat, lng, accuracy } - accuracy is null if the pin was moved by hand
let photoBlob = null;

// ------------------------------------------------------------------ panchayats
async function loadPanchayats() {
  const select = $("#panchayat");
  try {
    const res = await fetch("/api/panchayats");
    if (!res.ok) throw new Error();
    const list = await res.json();
    select.replaceChildren(new Option("Choose your area", ""));
    for (const p of list) select.add(new Option(p.name, p.id));
  } catch {
    select.replaceChildren(new Option("Could not load the list. Reload the page.", ""));
  }
}

// ------------------------------------------------------------------ map + GPS
function initMap() {
  map = L.map("map").setView(DEFAULT_CENTER, 12);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);

  marker = L.marker(DEFAULT_CENTER, { draggable: true });
  marker.on("dragend", () => {
    const p = marker.getLatLng();
    position = { lat: p.lat, lng: p.lng, accuracy: null };
    setGeoStatus("Pin placed by hand. Make sure it sits on the pothole.", "ok");
  });
  map.on("click", (e) => {
    marker.setLatLng(e.latlng).addTo(map);
    position = { lat: e.latlng.lat, lng: e.latlng.lng, accuracy: null };
    setGeoStatus("Pin placed by hand. Make sure it sits on the pothole.", "ok");
  });
}

function setGeoStatus(text, cls) {
  const el = $("#geo-status");
  el.textContent = text;
  el.className = "status" + (cls ? " " + cls : "");
}

function locate() {
  if (!("geolocation" in navigator)) {
    setGeoStatus("This browser cannot share your location. Tap the map to place the pin.", "warn");
    return;
  }
  setGeoStatus("Finding your location...");
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude, longitude, accuracy } = pos.coords;
      position = { lat: latitude, lng: longitude, accuracy };
      marker.setLatLng([latitude, longitude]).addTo(map);
      map.setView([latitude, longitude], 17);
      setGeoStatus("Location found, accurate to about " + Math.round(accuracy) + " m.", "ok");
    },
    (err) => {
      const msg = err.code === 1
        ? "Location permission was denied. Allow it in your browser settings, or tap the map to place the pin."
        : "Could not get your location. Tap the map to place the pin.";
      setGeoStatus(msg, "warn");
    },
    { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 }
  );
}

// ------------------------------------------------------------------ photo
// Shrinks the photo before upload so it works on slow mobile data.
async function shrink(file, maxSide = 1280) {
  let bitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
  } catch {
    return file;
  }
  const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext("2d").drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve) => canvas.toBlob((b) => resolve(b || file), "image/jpeg", 0.8));
}

$("#photo").addEventListener("change", async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  photoBlob = await shrink(file);
  const img = $("#photo-preview");
  img.src = URL.createObjectURL(photoBlob);
  img.hidden = false;
  $("#photo-hint").hidden = true;
  if (!position) locate(); // taking a photo is the natural moment to ask for location
});

// ------------------------------------------------------------------ submit
function showError(msg) {
  const el = $("#form-error");
  el.textContent = msg;
  el.hidden = !msg;
  if (msg) el.scrollIntoView({ behavior: "smooth", block: "center" });
}

$("#report-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  showError("");

  const description = $("#description").value.trim();
  if (!photoBlob) return showError("Add a photo of the pothole.");
  if (!position) return showError("We need the location. Tap \"Use my current location\" or tap the map.");
  if (!$("#panchayat").value) return showError("Choose your panchayat or municipality.");
  if (description.length < 5) return showError("Add a short description (at least 5 characters).");

  const data = new FormData();
  data.append("photo", photoBlob, "pothole.jpg");
  data.append("description", description);
  data.append("severity", $("#severity").value);
  data.append("landmark", $("#landmark").value.trim());
  data.append("latitude", position.lat);
  data.append("longitude", position.lng);
  if (position.accuracy != null) data.append("accuracy", position.accuracy);
  data.append("panchayatId", $("#panchayat").value);
  data.append("reporterName", $("#name").value.trim());
  data.append("reporterPhone", $("#phone").value.trim());

  const btn = $("#submit-btn");
  btn.disabled = true;
  btn.textContent = "Sending...";
  try {
    const res = await fetch("/api/reports", { method: "POST", body: data });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error || "Could not send the report. Please try again.");
    $("#ref").textContent = body.reference;
    $("#report-form").hidden = true;
    $("#done").hidden = false;
    window.scrollTo({ top: 0, behavior: "smooth" });
  } catch (err) {
    showError(err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = "Send report";
  }
});

$("#another-btn").addEventListener("click", () => {
  $("#report-form").reset();
  photoBlob = null;
  $("#photo-preview").hidden = true;
  $("#photo-hint").hidden = false;
  $("#done").hidden = true;
  $("#report-form").hidden = false;
  locate();
});

// ------------------------------------------------------------------ status check
const STATUS_TEXT = {
  REPORTED: "Received. Waiting for the panchayat to review it.",
  UNDER_REVIEW: "Under review by the panchayat.",
  IN_PROGRESS: "Repair work is in progress.",
  RESOLVED: "Resolved. The pothole has been fixed.",
  REJECTED: "Closed. The panchayat could not act on this report."
};

$("#track-btn").addEventListener("click", async () => {
  const out = $("#track-result");
  const id = $("#track-ref").value.replace(/\D/g, "");
  if (!id) { out.textContent = "Enter your reference number."; out.className = "status warn"; return; }
  try {
    const res = await fetch("/api/public/reports/" + id);
    if (!res.ok) throw new Error();
    const r = await res.json();
    out.textContent = r.panchayatName + ": " + STATUS_TEXT[r.status];
    out.className = "status ok";
  } catch {
    out.textContent = "No report found with that reference.";
    out.className = "status warn";
  }
});

// ------------------------------------------------------------------ start
loadPanchayats();
initMap();
locate();
$("#locate-btn").addEventListener("click", locate);
