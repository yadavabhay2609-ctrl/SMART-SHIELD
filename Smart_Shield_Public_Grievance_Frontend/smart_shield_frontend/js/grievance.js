const modal=document.getElementById("complaintModal");
const toast=document.getElementById("toast");
const openComplaint=()=>modal.classList.add("show");
document.getElementById("openComplaint").addEventListener("click",openComplaint);
document.querySelectorAll("[data-close]").forEach(b=>b.addEventListener("click",()=>modal.classList.remove("show")));
modal.addEventListener("click",e=>{if(e.target===modal)modal.classList.remove("show")});

document.getElementById("complaintForm").addEventListener("submit",e=>{
  e.preventDefault();
  const category=document.getElementById("category").value;
  const title=document.getElementById("title").value.trim();
  if(!category||!title)return;
  const id="SS-2026-"+String(Math.floor(1000+Math.random()*8999));
  const tr=document.createElement("tr");
  tr.dataset.search=(id+" "+title+" "+category).toLowerCase();
  tr.dataset.status="Pending";
  tr.innerHTML=`<td><b>${id}</b><span>${escapeHtml(title)}</span></td><td>${category}</td><td>02 Sep 2026</td><td><span class="status pending">Pending</span></td><td><span class="priority medium">Medium</span></td>`;
  document.getElementById("complaintTable").prepend(tr);
  document.getElementById("totalCount").textContent=String(Number(document.getElementById("totalCount").textContent)+1).padStart(2,"0");
  document.getElementById("complaintForm").reset();
  modal.classList.remove("show");
  showToast("✓ Complaint submitted: "+id);
});
function escapeHtml(s){return s.replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]));}
function showToast(msg){toast.textContent=msg;toast.classList.add("show");setTimeout(()=>toast.classList.remove("show"),3000)}

const search=document.getElementById("searchInput"), filter=document.getElementById("statusFilter");
function filterRows(){
  const q=search.value.toLowerCase(), f=filter.value;
  document.querySelectorAll("#complaintTable tr").forEach(r=>{
    r.style.display=((r.dataset.search||"").includes(q)&&(f==="all"||r.dataset.status===f))?"":"none";
  });
}
search.addEventListener("input",filterRows); filter.addEventListener("change",filterRows);
document.getElementById("viewAll").addEventListener("click",()=>{search.value="";filter.value="all";filterRows();document.getElementById("complaints").scrollIntoView({behavior:"smooth"})});
document.getElementById("sosBtn").addEventListener("click",()=>showToast("SOS demo alert ready — connect this to your backend/API"));
document.getElementById("assistantBtn").addEventListener("click",()=>showToast("AI Legal Assistant demo — API integration comes next"));

const sidebar=document.getElementById("sidebar");
document.getElementById("menuBtn").addEventListener("click",()=>sidebar.classList.toggle("open"));
document.querySelectorAll(".nav-item").forEach(a=>a.addEventListener("click",()=>sidebar.classList.remove("open")));
