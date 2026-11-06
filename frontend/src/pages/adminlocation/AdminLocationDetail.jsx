import { useState } from 'react';
import AdminLocationForm from "./AdminLocationForm.jsx";

export default function AdminLocationDetail({place, onEdit, onRemove, onClose}) {
    const [editing, setEditing] = useState(false);

    if (editing){
        return (
            <AdminLocationForm initial = {place}
                               onSave={async (form) => {await  onEdit(place.locationId, form);
                                   setEditing(false);}}
                               onCancel={() => setEditing(false)}
            />
        );
    }

    const askRemove = () => window.confirmModal({
        title:'장소 삭제',
        messages:place.locationName+'을(를) 삭제할까요 ?',
        confirm:'삭제',
        danger:true
    }).then((ok) => ok && onRemove(place.locationId));
    return (
        <div>
            <div>사진 들어갈곳</div>
            <div>
                <h3>{place.locationName}</h3>
                <p className="hint">{place.locationCategory}·{place.locationAddress}</p>
                <p>{place.locationDescription}</p>
            </div>
            <div className="form-actions">
                <button type="button" className="btn btn-line" onClick={onClose}>닫기</button>
                <button type="button" className="btn btn-line" onClick={()=> setEditing(true)}>수정</button>
                <button type="button" className="btn btn-danger" onClick={askRemove}>삭제</button>
            </div>
        </div>
    );
}